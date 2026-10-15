package com.olivapp.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.olivapp.app.voz.data.AlmacenNotasVoz
import com.olivapp.app.voz.data.BaseNotasVoz
import com.olivapp.app.voz.data.NotaVozFila
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests del aislamiento de la cola de notas de voz por cuenta (feature
 * aislamiento-de-cola-por-socio) sobre Room real simulado por Robolectric.
 *
 * La cola es del dispositivo: con dos cuentas en el mismo teléfono, cada socio
 * solo ve y envía las notas suyas, y las notas de una cuenta siguen ahí cuando
 * se vuelve a entrar con ella.
 */
@RunWith(RobolectricTestRunner::class)
class ColaNotasPorCuentaTest {

    private lateinit var contexto: Context

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
    }

    /** La cola tal como la ve una cuenta concreta del teléfono. */
    private fun colaDe(idDeLaCuenta: String) = AlmacenNotasVoz(contexto) { idDeLaCuenta }

    private fun notaEn(almacen: AlmacenNotasVoz, nombre: String): AlmacenNotasVoz.NotaDeVoz {
        val fichero = File(almacen.nuevaRutaDeAudio().parentFile, nombre)
        return almacen.guardar(fichero, 5)
    }

    @Test
    fun `cada socio ve solo sus notas en el mismo telefono`() {
        val ana = colaDe("cuenta-ana-${System.nanoTime()}")
        val luis = colaDe("cuenta-luis-${System.nanoTime()}")
        val notaDeAna = notaEn(ana, "nota-ana-${System.nanoTime()}.m4a")
        val notaDeLuis = notaEn(luis, "nota-luis-${System.nanoTime()}.m4a")

        assertEquals(listOf(notaDeAna.id), ana.notas().map { it.id })
        assertEquals(listOf(notaDeLuis.id), luis.notas().map { it.id })
    }

    @Test
    fun `la cola pendiente solo recoge las notas del socio con la sesion activa`() {
        val ana = colaDe("cuenta-ana-${System.nanoTime()}")
        val luis = colaDe("cuenta-luis-${System.nanoTime()}")
        val notaDeAna = notaEn(ana, "nota-ana-${System.nanoTime()}.m4a")
        notaEn(luis, "nota-luis-${System.nanoTime()}.m4a")

        val pendientesDeAna = ana.pendientesEnOrdenDeGrabacion().map { it.id }

        // Con el token de Ana solo se enviarían las notas de Ana.
        assertEquals(listOf(notaDeAna.id), pendientesDeAna)
    }

    @Test
    fun `al volver a una cuenta sus notas siguen ahi`() {
        val idAna = "cuenta-ana-${System.nanoTime()}"
        val ana = colaDe(idAna)
        val notaDeAna = notaEn(ana, "nota-ana-${System.nanoTime()}.m4a")
        val luis = colaDe("cuenta-luis-${System.nanoTime()}")
        notaEn(luis, "nota-luis-${System.nanoTime()}.m4a")

        assertTrue(colaDe(idAna).notas().map { it.id }.contains(notaDeAna.id))
    }

    @Test
    fun `las notas sin propietario no se enseñan ni se envian con una cuenta activa`() {
        val cuenta = "cuenta-${System.nanoTime()}"
        val almacen = colaDe(cuenta)
        val idSinDueno = "nota-sin-dueno-${System.nanoTime()}.m4a"
        BaseNotasVoz.de(contexto).dao().insertar(
            NotaVozFila(
                id = idSinDueno, fichero = "/f/sin-dueno.m4a",
                creadaEn = "2026-01-01T10:00:00", segundos = 5,
                estado = "pendiente", texto = ""
            )
        )

        assertFalse(almacen.notas().map { it.id }.contains(idSinDueno))
        assertFalse(almacen.pendientesEnOrdenDeGrabacion().map { it.id }.contains(idSinDueno))
        // No se pierde: sigue en el teléfono, a la espera de un dueño.
        val guardada = BaseNotasVoz.de(contexto).dao().notas("")
            .first { it.id == idSinDueno }
        assertEquals("", guardada.dueno)
        assertEquals("pendiente", guardada.estado)
    }
}
