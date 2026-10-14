package com.olivapp.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests de caracterización de la cola local de notas de voz (feature
 * cola-offline-de-notas) sobre Room real simulado por Robolectric.
 *
 * Fijan la persistencia, el orden de la cola, las transiciones de estado y la
 * migración del índice JSON antiguo antes del refactor de arquitectura.
 */
@RunWith(RobolectricTestRunner::class)
class ColaNotasCaracterizacionTest {

    private lateinit var contexto: Context
    private lateinit var almacen: AlmacenNotasVoz

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        almacen = AlmacenNotasVoz(contexto)
    }

    private fun notaNueva(sufijo: String, segundos: Int = 6): AlmacenNotasVoz.NotaDeVoz {
        val fichero = File(almacen.nuevaRutaDeAudio().parentFile, "nota-test-$sufijo.m4a")
        return almacen.guardar(fichero, segundos)
    }

    @Test
    fun `la nota grabada se registra pendiente con su audio asociado`() {
        val fichero = almacen.nuevaRutaDeAudio()
        assertTrue(fichero.name.startsWith("nota-"))
        assertTrue(fichero.name.endsWith(".m4a"))

        val nota = almacen.guardar(fichero, 6)

        assertEquals("pendiente", nota.estado)
        assertEquals(6, nota.segundos)
        assertEquals("", nota.texto)
        assertEquals(fichero.absolutePath, nota.fichero)
    }

    @Test
    fun `la lista va de la nota mas reciente a la mas antigua`() {
        val dao = BaseNotasVoz.de(contexto).dao()
        val idVieja = "nota-test-vieja-${System.nanoTime()}.m4a"
        val idNueva = "nota-test-nueva-${System.nanoTime()}.m4a"
        dao.insertar(
            NotaVozFila(
                id = idVieja, fichero = "/f/vieja.m4a", creadaEn = "2026-01-01T10:00:00",
                segundos = 3, estado = "pendiente", texto = ""
            )
        )
        dao.insertar(
            NotaVozFila(
                id = idNueva, fichero = "/f/nueva.m4a", creadaEn = "2026-01-02T10:00:00",
                segundos = 5, estado = "pendiente", texto = ""
            )
        )

        val ids = almacen.notas().map { it.id }

        assertTrue(ids.indexOf(idNueva) < ids.indexOf(idVieja))
    }

    @Test
    fun `las pendientes van en orden de grabacion y solo las pendientes`() {
        val dao = BaseNotasVoz.de(contexto).dao()
        val idNueva = "nota-test-pend-nueva-${System.nanoTime()}.m4a"
        val idVieja = "nota-test-pend-vieja-${System.nanoTime()}.m4a"
        val idTranscrita = "nota-test-pend-transcrita-${System.nanoTime()}.m4a"
        dao.insertar(
            NotaVozFila(
                id = idNueva, fichero = "/f/n.m4a", creadaEn = "2026-01-02T10:00:00",
                segundos = 5, estado = "pendiente", texto = ""
            )
        )
        dao.insertar(
            NotaVozFila(
                id = idVieja, fichero = "/f/v.m4a", creadaEn = "2026-01-01T10:00:00",
                segundos = 48, estado = "pendiente", texto = ""
            )
        )
        dao.insertar(
            NotaVozFila(
                id = idTranscrita, fichero = "/f/t.m4a", creadaEn = "2026-01-03T10:00:00",
                segundos = 3, estado = "transcrita", texto = "Hecho."
            )
        )

        // La cola es un singleton compartido entre tests: se miran solo las
        // notas de este test (los ids de otras pruebas también están en Room).
        val ids = almacen.pendientesEnOrdenDeGrabacion()
            .map { it.id }
            .filter { it == idVieja || it == idNueva }

        assertEquals(listOf(idVieja, idNueva), ids)
    }

    @Test
    fun `marcar guarda el texto y el borrador junto al estado`() {
        val nota = notaNueva("marcar-${System.nanoTime()}")
        val borrador = """{"fecha": "2026-01-20"}"""

        almacen.marcar(nota.id, "transcrita", "He regado.", borrador)

        val guardada = almacen.notas().first { it.id == nota.id }
        assertEquals("transcrita", guardada.estado)
        assertEquals("He regado.", guardada.texto)
        assertEquals(borrador, guardada.borrador)
    }

    @Test
    fun `actualizar el borrador no toca ni el estado ni el texto`() {
        val nota = notaNueva("borrador-${System.nanoTime()}")
        almacen.marcar(nota.id, "transcrita", "He regado.", "")

        almacen.actualizarBorrador(nota.id, """{"producto": "cobre"}""")

        val guardada = almacen.notas().first { it.id == nota.id }
        assertEquals("transcrita", guardada.estado)
        assertEquals("He regado.", guardada.texto)
        assertEquals("""{"producto": "cobre"}""", guardada.borrador)
    }

    @Test
    fun `el indice JSON antiguo se importa a Room y se borra`() {
        val idImportada = "nota-test-importada-${System.nanoTime()}.m4a"
        val indice = File(contexto.filesDir, "notas-voz.json")
        indice.writeText(
            JSONArray()
                .put(
                    JSONObjectDePrueba(
                        id = idImportada,
                        fichero = "/f/importada.m4a",
                        creadaEn = "2026-01-01T10:00:00",
                        segundos = 7,
                        estado = "pendiente",
                        texto = ""
                    )
                )
                .toString()
        )

        val almacenNuevo = AlmacenNotasVoz(contexto)

        val importada = almacenNuevo.notas().firstOrNull { it.id == idImportada }
        assertNotNull(importada)
        assertEquals("pendiente", importada!!.estado)
        assertEquals(7, importada.segundos)
        assertEquals("/f/importada.m4a", importada.fichero)
        assertFalse(indice.exists())
    }

    @Test
    fun `el estado visible de la nota es el texto que ve el socio`() {
        fun nota(estado: String) = AlmacenNotasVoz.NotaDeVoz(
            id = "n", fichero = "/f", cuando = "01/01/2026 10:00", segundos = 3,
            estado = estado, texto = "", borrador = ""
        )

        assertEquals(
            "Pendiente de enviar. Está en la cola del teléfono.",
            nota("pendiente").estadoVisible
        )
        assertEquals("Transcribiendo…", nota("transcribiendo").estadoVisible)
        assertEquals("Transcrita", nota("transcrita").estadoVisible)
        assertEquals(
            "Confirmada. La labor ya está en Mi cuaderno.",
            nota("confirmada").estadoVisible
        )
        assertEquals(
            "Descartada. No se ha creado ninguna labor.",
            nota("descartada").estadoVisible
        )
        assertEquals(
            "No se ha podido transcribir. Toca para reintentar.",
            nota("error").estadoVisible
        )
        assertEquals(
            "Este audio no se puede transcribir. Vuelve a grabar la nota.",
            nota("audio_invalido").estadoVisible
        )
        assertEquals("Pendiente de transcripción", nota("lo_que_sea").estadoVisible)
    }

    private fun JSONObjectDePrueba(
        id: String,
        fichero: String,
        creadaEn: String,
        segundos: Int,
        estado: String,
        texto: String
    ): org.json.JSONObject = org.json.JSONObject()
        .put("id", id)
        .put("fichero", fichero)
        .put("creada_en", creadaEn)
        .put("segundos", segundos)
        .put("estado", estado)
        .put("texto", texto)
}
