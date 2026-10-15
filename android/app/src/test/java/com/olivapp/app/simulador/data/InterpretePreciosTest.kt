package com.olivapp.app.simulador.data

import com.olivapp.app.simulador.domain.CategoriaAceite
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Lectura de las respuestas del backend de precios (feature simulador-avanzado-precio-aceite). */
@RunWith(RobolectricTestRunner::class)
class InterpretePreciosTest {

    private val delta = 1e-9

    @Test
    fun `lee las semanas de una campana con las tres categorias`() {
        val json = JSONObject(
            """
            {"categoria":"aove","mercado":"jaen","campana":"2025/2026",
             "semanas":[
               {"semana":"2026-09-28","semana_fin":"2026-10-04","numero_semana":53,
                "campana":"2025/2026","precio":3.42,
                "precios":{"aove":3.42,"lampante":3.05,"virgen":3.18}},
               {"semana":"2026-09-21","semana_fin":"2026-09-27","numero_semana":52,
                "campana":"2025/2026","precio":3.42,
                "precios":{"aove":3.42,"lampante":3.05}}],
             "fuente":"Comisión Europea · Agri-food Data Portal",
             "desactualizado":false,"consultado_en":"2026-10-15 08:00:00","antiguedad_horas":2}
            """.trimIndent()
        )

        val referencia = InterpretePrecios.semanas(json)

        assertEquals(2, referencia.semanas.size)
        val primera = referencia.semanas[0]
        assertEquals("2026-09-28", primera.semana)
        assertEquals("2026-10-04", primera.semanaFin)
        assertEquals(53, primera.numeroSemana)
        assertEquals("2025/2026", primera.campana)
        assertEquals(3.42, primera.precios[CategoriaAceite.AOVE]!!, delta)
        assertEquals(3.18, primera.precios[CategoriaAceite.VIRGEN]!!, delta)
        assertEquals(3.05, primera.precios[CategoriaAceite.LAMPANTE]!!, delta)
        // Una semana sin virgen no inventa el precio que falta.
        assertNull(referencia.semanas[1].precios[CategoriaAceite.VIRGEN])
        assertEquals("Comisión Europea · Agri-food Data Portal", referencia.fuente)
        assertFalse(referencia.desactualizado)
        assertEquals(2, referencia.antiguedadHoras)
    }

    @Test
    fun `una respuesta desde la cache lo indica con su antiguedad`() {
        val json = JSONObject(
            """{"semanas":[],"fuente":"F","desactualizado":true,"antiguedad_horas":30}"""
        )

        val referencia = InterpretePrecios.semanas(json)

        assertTrue(referencia.desactualizado)
        assertEquals(30, referencia.antiguedadHoras)
        assertTrue(referencia.semanas.isEmpty())
    }

    @Test
    fun `sin antiguedad en la respuesta queda en nulo`() {
        val referencia = InterpretePrecios.semanas(JSONObject("""{"semanas":[]}"""))

        assertNull(referencia.antiguedadHoras)
    }

    @Test
    fun `lee la ultima semana como una lista de una semana`() {
        val json = JSONObject(
            """
            {"mercado":"media-nacional","semana":"2026-10-05","semana_fin":"2026-10-11",
             "numero_semana":1,"campana":"2026/2027",
             "precios":{"aove":4.1,"virgen":3.8,"lampante":3.4},
             "fuente":"Comisión Europea · Agri-food Data Portal","desactualizado":false,
             "consultado_en":"2026-10-15 08:00:00","antiguedad_horas":0}
            """.trimIndent()
        )

        val referencia = InterpretePrecios.ultimaSemana(json)

        assertNotNull(referencia)
        assertEquals(1, referencia!!.semanas.size)
        assertEquals("2026-10-05", referencia.semanas[0].semana)
        assertEquals("2026/2027", referencia.semanas[0].campana)
        assertEquals(4.1, referencia.semanas[0].precios[CategoriaAceite.AOVE]!!, delta)
    }

    @Test
    fun `una ultima semana sin precios es ilegible`() {
        assertNull(InterpretePrecios.ultimaSemana(JSONObject("""{"semana":"2026-10-05"}""")))
        assertNull(
            InterpretePrecios.ultimaSemana(
                JSONObject("""{"semana":"2026-10-05","precios":{}}""")
            )
        )
    }

    @Test
    fun `las semanas sin fecha o sin precios se descartan`() {
        val json = JSONObject(
            """
            {"semanas":[{"semana":"","precios":{"aove":3.0}},
                        {"semana":"2026-09-28","precios":{}},
                        {"semana":"2026-09-21","precios":{"aove":3.0}}]}
            """.trimIndent()
        )

        val referencia = InterpretePrecios.semanas(json)

        assertEquals(listOf("2026-09-21"), referencia.semanas.map { it.semana })
    }
}
