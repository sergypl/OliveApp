package com.olivapp.app.cuaderno.domain

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Con Robolectric como el resto de la batería: los modelos de labor llevan
// campos JSON (org.json), que en JVM sin Robolectric no está implementado.
@RunWith(RobolectricTestRunner::class)
/** Tests de la lógica pura de la línea de tiempo (feature cuaderno-linea-de-tiempo). */
class LineaDeTiempoTest {

    private fun labor(
        id: String,
        fechaIso: String,
        categoria: String,
        parcela: String
    ) = DatosLabor(
        fecha = fechaIso,
        categoria = categoria,
        parcela = parcela,
        campos = emptyList(),
        observaciones = "",
        editable = LaborEditable(
            id = id,
            fechaVisible = fechaIso,
            fechaIso = fechaIso,
            parcelaId = "p-" + parcela,
            parcelaNombre = parcela,
            categoria = categoria,
            campos = JSONObject(),
            observaciones = ""
        )
    )

    private fun campanaDeEjemplo() = DatosCampanaDeLabores(
        nombre = "2025/2026",
        esActual = true,
        labores = listOf(
            labor("lab-1", "2025-12-20", "tratamiento", "Cañada Honda"),
            labor("lab-2", "2025-11-03", "riego", "La Noria"),
            labor("lab-3", "2025-12-01", "cosecha", "Cañada Honda"),
            labor("lab-4", "2026-01-09", "tratamiento", "La Noria")
        )
    )

    @Test
    fun `las labores se agrupan por mes y cada mes va de lo mas reciente a lo mas antiguo`() {
        val grupos = agruparPorMes(listOf(campanaDeEjemplo()))

        assertEquals(listOf("2026-01", "2025-12", "2025-11"), grupos[0].meses.map { it.clave })
        assertEquals(
            listOf("lab-4"),
            grupos[0].meses[0].labores.map { it.editable.id }
        )
        assertEquals(
            listOf("lab-1", "lab-3"),
            grupos[0].meses[1].labores.map { it.editable.id }
        )
    }

    @Test
    fun `la cabecera del mes se titula en espanol con su ano`() {
        assertEquals("Diciembre de 2025", tituloDeMes("2025-12"))
        assertEquals("Enero de 2026", tituloDeMes("2026-01"))
        assertEquals("Sin fecha", tituloDeMes(""))
        assertEquals("Sin fecha", tituloDeMes("2025-13"))
    }

    @Test
    fun `la campaña conserva su nombre y si es la actual`() {
        val grupos = agruparPorMes(listOf(campanaDeEjemplo()))

        assertEquals("2025/2026", grupos[0].nombre)
        assertTrue(grupos[0].esActual)
    }

    @Test
    fun `el filtro por categoria deja solo las labores de esa categoria`() {
        val filtradas = filtrarCampanas(
            listOf(campanaDeEjemplo()),
            FiltroDeCuaderno(categoria = "riego")
        )

        assertEquals(listOf("lab-2"), filtradas[0].labores.map { it.editable.id })
    }

    @Test
    fun `el filtro por parcela deja solo las labores de esa parcela`() {
        val filtradas = filtrarCampanas(
            listOf(campanaDeEjemplo()),
            FiltroDeCuaderno(parcela = "Cañada Honda")
        )

        assertEquals(listOf("lab-1", "lab-3"), filtradas[0].labores.map { it.editable.id })
    }

    @Test
    fun `los dos filtros se aplican a la vez`() {
        val filtradas = filtrarCampanas(
            listOf(campanaDeEjemplo()),
            FiltroDeCuaderno(categoria = "tratamiento", parcela = "La Noria")
        )

        assertEquals(listOf("lab-4"), filtradas[0].labores.map { it.editable.id })
    }

    @Test
    fun `las campañas que se quedan sin labores desaparecen del resultado`() {
        val filtradas = filtrarCampanas(
            listOf(
                campanaDeEjemplo(),
                DatosCampanaDeLabores(
                    nombre = "2024/2025",
                    esActual = false,
                    labores = listOf(labor("lab-9", "2025-02-01", "riego", "La Noria"))
                )
            ),
            FiltroDeCuaderno(categoria = "cosecha")
        )

        assertEquals(listOf("2025/2026"), filtradas.map { it.nombre })
    }

    @Test
    fun `sin filtro se enseñan todas las labores y el filtro se ve vacío`() {
        val todas = filtrarCampanas(listOf(campanaDeEjemplo()), FiltroDeCuaderno())

        assertEquals(4, todas[0].labores.size)
        assertTrue(FiltroDeCuaderno().sinFiltros)
        assertTrue(!FiltroDeCuaderno(categoria = "riego").sinFiltros)
    }

    @Test
    fun `las opciones de filtro salen del cuaderno y van en orden fijo`() {
        val opciones = opcionesDeFiltro(listOf(campanaDeEjemplo()))

        assertEquals(listOf("tratamiento", "riego", "cosecha"), opciones.categorias)
        assertEquals(listOf("Cañada Honda", "La Noria"), opciones.parcelas)
    }

    @Test
    fun `solo se ofrecen las categorias y parcelas que hay en el cuaderno`() {
        val opciones = opcionesDeFiltro(
            listOf(
                DatosCampanaDeLabores(
                    nombre = "2025/2026",
                    esActual = true,
                    labores = listOf(labor("lab-1", "2025-12-20", "riego", "La Noria"))
                )
            )
        )

        assertEquals(listOf("riego"), opciones.categorias)
        assertEquals(listOf("La Noria"), opciones.parcelas)
    }
}
