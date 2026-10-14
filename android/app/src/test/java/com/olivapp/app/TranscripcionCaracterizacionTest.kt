package com.olivapp.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.olivapp.app.voz.data.AlmacenNotasVoz
import com.olivapp.app.voz.domain.aplicarTranscripcion
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests de caracterización de la tabla de decisiones de `aplicarTranscripcion`
 * (features transcripcion-de-nota-voz, cola-offline-de-notas y
 * reintento-de-transcripcion-fallida).
 *
 * Fijan qué estado queda en la cola y qué aviso recibe el socio en cada salida
 * del backend antes del refactor de arquitectura.
 */
@RunWith(RobolectricTestRunner::class)
class TranscripcionCaracterizacionTest {

    private lateinit var contexto: Context
    private lateinit var almacen: AlmacenNotasVoz

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        almacen = AlmacenNotasVoz(contexto)
    }

    private fun notaPendiente(): AlmacenNotasVoz.NotaDeVoz {
        val fichero = File(almacen.nuevaRutaDeAudio().parentFile, "nota-t-${System.nanoTime()}.m4a")
        return almacen.guardar(fichero, 5)
    }

    @Test
    fun `con texto la nota queda transcrita con su borrador`() {
        val nota = notaPendiente()
        val borrador = """{"fecha": "2026-01-20"}"""

        val aviso = aplicarTranscripcion(
            almacen, nota.id,
            texto = "He regado.", borrador = borrador,
            error = "", sinConexion = false, audioInvalido = false
        )

        assertEquals("Nota transcrita. Ya puedes leer el texto.", aviso)
        val guardada = almacen.notas().first { it.id == nota.id }
        assertEquals("transcrita", guardada.estado)
        assertEquals("He regado.", guardada.texto)
        assertEquals(borrador, guardada.borrador)
    }

    @Test
    fun `sin conexion la nota vuelve a pendiente y se avisa que esta guardada`() {
        val nota = notaPendiente()

        val aviso = aplicarTranscripcion(
            almacen, nota.id,
            texto = null, borrador = "",
            error = "No hay conexión con el servidor. Inténtalo de nuevo.",
            sinConexion = true, audioInvalido = false
        )

        assertEquals(
            "Sin conexión con el servidor. Tu nota está guardada en el teléfono, pendiente de enviar.",
            aviso
        )
        val guardada = almacen.notas().first { it.id == nota.id }
        assertEquals("pendiente", guardada.estado)
    }

    @Test
    fun `con audio invalido la nota pide repetir el dictado`() {
        val nota = notaPendiente()
        val error = "Este audio no se puede transcribir. Vuelve a grabar la nota."

        val aviso = aplicarTranscripcion(
            almacen, nota.id,
            texto = null, borrador = "",
            error = error, sinConexion = false, audioInvalido = true
        )

        assertEquals(error, aviso)
        val guardada = almacen.notas().first { it.id == nota.id }
        assertEquals("audio_invalido", guardada.estado)
    }

    @Test
    fun `con fallo del servicio la nota queda en error reintentable`() {
        val nota = notaPendiente()
        val error = "No se ha podido transcribir. Toca para reintentar."

        val aviso = aplicarTranscripcion(
            almacen, nota.id,
            texto = null, borrador = "",
            error = error, sinConexion = false, audioInvalido = false
        )

        assertEquals(error, aviso)
        val guardada = almacen.notas().first { it.id == nota.id }
        assertEquals("error", guardada.estado)
    }
}
