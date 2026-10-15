package com.olivapp.app

import com.olivapp.app.voz.domain.camposFaltantesDeBorrador
import com.olivapp.app.voz.domain.cantidadVisible
import com.olivapp.app.voz.domain.etiquetaDeCampo
import com.olivapp.app.voz.domain.fechaIsoDeBorrador
import com.olivapp.app.voz.domain.lineasDeBorrador
import com.olivapp.app.voz.domain.numeroVisible
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests de caracterización de la lógica del borrador IA (features
 * borrador-ia-desde-transcripcion, completar-borrador-por-preguntas,
 * borrador-sin-parcela y fecha-relativa-en-borrador).
 *
 * Fijan los textos y reglas verificados en el móvil antes del refactor.
 */
@RunWith(RobolectricTestRunner::class)
class BorradorCaracterizacionTest {

    @Test
    fun `la fecha ISO valida se conserva y las expresiones sin dia concreto quedan vacias`() {
        assertEquals("2026-01-20", fechaIsoDeBorrador(JSONObject("""{"fecha": "2026-01-20"}""")))
        assertEquals("", fechaIsoDeBorrador(JSONObject("""{"fecha": "hoy"}""")))
        assertEquals("", fechaIsoDeBorrador(JSONObject("""{"fecha": "la semana pasada"}""")))
        assertEquals("", fechaIsoDeBorrador(JSONObject("""{"fecha": "20/01/2026"}""")))
    }

    @Test
    fun `la fecha del borrador se valida solo de forma`() {
        // "2026-02-30" pasa tal cual el filtro de forma; quien no puede con
        // ella es idCampanaDeLaFecha, que la rechaza sin reventar.
        assertEquals("2026-02-30", fechaIsoDeBorrador(JSONObject("""{"fecha": "2026-02-30"}""")))
    }

    @Test
    fun `el tratamiento completo no tiene campos faltantes`() {
        val borrador = JSONObject(
            """
            {"categoria": "tratamiento", "campos": {"producto": "cobre",
             "plaga_a_controlar": "mosca del olivo", "cantidad": 2, "unidad": "L"}}
            """.trimIndent()
        )

        assertEquals(emptyList<String>(), camposFaltantesDeBorrador(borrador))
    }

    @Test
    fun `los campos faltantes de un tratamiento salen en el orden de la ficha del dominio`() {
        val borrador = JSONObject(
            """{"categoria": "tratamiento", "campos": {"plaga_a_controlar": "cochinilla"}}"""
        )

        assertEquals(
            listOf("producto", "cantidad", "unidad"),
            camposFaltantesDeBorrador(borrador)
        )
    }

    @Test
    fun `riego exige cantidad y unidad y cosecha modalidad y kilos`() {
        assertEquals(
            listOf("cantidad", "unidad"),
            camposFaltantesDeBorrador(JSONObject("""{"categoria": "riego", "campos": {}}"""))
        )
        assertEquals(
            listOf("modalidad", "kilos"),
            camposFaltantesDeBorrador(JSONObject("""{"categoria": "cosecha", "campos": {}}"""))
        )
    }

    @Test
    fun `un campo en blanco cuenta como faltante y la categoria desconocida no exige nada`() {
        val borrador = JSONObject(
            """{"categoria": "riego", "campos": {"cantidad": "  ", "unidad": "horas"}}"""
        )

        assertEquals(listOf("cantidad"), camposFaltantesDeBorrador(borrador))
        assertEquals(
            emptyList<String>(),
            camposFaltantesDeBorrador(JSONObject("""{"categoria": "poda"}"""))
        )
    }

    @Test
    fun `la ficha del tratamiento marca con (falta) lo que no trae el dictado`() {
        val borrador = JSONObject(
            """
            {"fecha": "2026-01-20", "parcela": "", "categoria": "tratamiento",
             "campos": {"plaga_a_controlar": "mosca del olivo"}}
            """.trimIndent()
        )

        val lineas = lineasDeBorrador(borrador)

        assertEquals(
            listOf(
                "Fecha" to "20/01/2026",
                "Parcela" to "Sin identificar",
                "Categoría" to "Tratamiento fitosanitario",
                "Producto" to "(falta)",
                "Plaga a controlar" to "mosca del olivo",
                "Cantidad" to "(falta)"
            ),
            lineas
        )
    }

    @Test
    fun `la ficha de cosecha enseña modalidad y kilos y añade observaciones si las hay`() {
        val borrador = JSONObject(
            """
            {"fecha": "2026-03-13", "parcela": "Cañada Honda", "categoria": "cosecha",
             "campos": {"modalidad": "a vuelo", "kilos": 850.0},
             "observaciones": "Buen rendimiento."}
            """.trimIndent()
        )

        val lineas = lineasDeBorrador(borrador)

        assertEquals("Modalidad" to "a vuelo", lineas[3])
        assertEquals("Kilos" to "850", lineas[4])
        assertEquals("Observaciones" to "Buen rendimiento.", lineas[5])
    }

    @Test
    fun `sin observaciones la ficha no lleva la linea de observaciones`() {
        val borrador = JSONObject(
            """{"fecha": "2026-01-20", "parcela": "La Noria", "categoria": "riego",
               "campos": {"cantidad": 2.5, "unidad": "horas"}, "observaciones": "  "}"""
        )

        val lineas = lineasDeBorrador(borrador)

        assertEquals(listOf("Fecha", "Parcela", "Categoría", "Cantidad"), lineas.map { it.first })
        // El borrador enseña el número tal cual viene (solo quita el '.0'):
        // 2.5 se lee '2.5' (el formato español con coma es de 'Mi cuaderno').
        assertEquals("Cantidad" to "2.5 horas", lineas[3])
    }

    @Test
    fun `las etiquetas de los campos son las del socio`() {
        assertEquals("Producto", etiquetaDeCampo("producto"))
        assertEquals("Plaga a controlar", etiquetaDeCampo("plaga_a_controlar"))
        assertEquals("Cantidad", etiquetaDeCampo("cantidad"))
        assertEquals("Unidad", etiquetaDeCampo("unidad"))
        assertEquals("Modalidad", etiquetaDeCampo("modalidad"))
        assertEquals("Kilos", etiquetaDeCampo("kilos"))
        assertEquals("otro_campo", etiquetaDeCampo("otro_campo"))
    }

    @Test
    fun `los numeros sin decimales de mas se enseñan sin punto cero`() {
        assertEquals("2", numeroVisible(2.0))
        assertEquals("2,5", numeroVisible("2,5"))
        assertEquals("", numeroVisible(null))
    }

    @Test
    fun `la cantidad lleva su unidad y sin unidad solo el numero`() {
        val campos = JSONObject("""{"cantidad": 2.0, "unidad": "L"}""")

        assertEquals("2 L", cantidadVisible(campos, "cantidad", "unidad"))
        assertEquals("2", cantidadVisible(JSONObject("""{"cantidad": 2.0}"""), "cantidad", "unidad"))
    }
}
