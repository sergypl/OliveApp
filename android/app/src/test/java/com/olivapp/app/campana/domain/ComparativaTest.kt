package com.olivapp.app.campana.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests del cálculo de la comparativa del panel (feature panel-moderno). */
class ComparativaTest {

    private fun actual(kg: Double?) =
        DatosCampanaResumen(id = "2025-2026", nombre = "2025/2026", estado = "actual", aceiteKg = kg)

    private fun cerrada(anyo: Int, kg: Double?) = DatosCampanaResumen(
        id = "$anyo-${anyo + 1}",
        nombre = "$anyo/${anyo + 1}",
        estado = "cerrada",
        aceiteKg = kg
    )

    @Test
    fun `compara la actual con la cerrada mas reciente`() {
        val campanas = listOf(actual(1774.0), cerrada(2024, 1198.0), cerrada(2023, 900.0))

        val comparativa = calcularComparativa(campanas)!!

        assertEquals("2024/2025", comparativa.campanaAnterior)
        assertEquals("24/25", comparativa.etiquetaAnterior)
        assertEquals(48, comparativa.variacion) // 47,9 % redondeado
        assertEquals("+48 %", comparativa.textoVariacion)
        assertEquals(true, comparativa.sube)
    }

    @Test
    fun `si baja el aceite la variacion es negativa y sube es false`() {
        val comparativa = calcularComparativa(listOf(actual(600.0), cerrada(2024, 1200.0)))!!

        assertEquals(-50, comparativa.variacion)
        assertEquals("−50 %", comparativa.textoVariacion)
        assertEquals(false, comparativa.sube)
    }

    @Test
    fun `sin campana anterior no hay comparativa`() {
        assertNull(calcularComparativa(listOf(actual(1774.0))))
        assertNull(calcularComparativa(emptyList()))
    }

    @Test
    fun `sin campana actual o sin cifras no hay comparativa`() {
        assertNull(calcularComparativa(listOf(cerrada(2024, 1198.0))))
        assertNull(calcularComparativa(listOf(actual(null), cerrada(2024, 1198.0))))
        assertNull(calcularComparativa(listOf(actual(1774.0), cerrada(2024, null))))
    }

    @Test
    fun `con la anterior a cero no se puede calcular el porcentaje`() {
        val comparativa = calcularComparativa(listOf(actual(1774.0), cerrada(2024, 0.0)))!!

        assertNull(comparativa.variacion)
        assertNull(comparativa.textoVariacion)
        assertNull(comparativa.sube)
    }

    @Test
    fun `con la actual a cero tampoco se calcula el porcentaje`() {
        val comparativa = calcularComparativa(listOf(actual(0.0), cerrada(2024, 1198.0)))!!

        assertNull(comparativa.variacion)
        assertNull(comparativa.sube)
    }

    @Test
    fun `con las dos a cero no hay variacion`() {
        val comparativa = calcularComparativa(listOf(actual(0.0), cerrada(2024, 0.0)))!!

        assertNull(comparativa.variacion)
    }

    @Test
    fun `el mismo aceite da 0 por ciento y no sube ni baja`() {
        val comparativa = calcularComparativa(listOf(actual(1000.0), cerrada(2024, 1000.0)))!!

        assertEquals(0, comparativa.variacion)
        assertEquals("0 %", comparativa.textoVariacion)
        assertNull(comparativa.sube)
    }

    @Test
    fun `el porcentaje se redondea al entero mas cercano con los medios alejandose del cero`() {
        assertEquals(48, redondearPorcentaje(47.9))
        assertEquals(47, redondearPorcentaje(47.4))
        assertEquals(3, redondearPorcentaje(2.5))
        assertEquals(-3, redondearPorcentaje(-2.5))
        assertEquals(-48, redondearPorcentaje(-47.6))
        assertEquals(0, redondearPorcentaje(0.4))
        assertEquals(0, redondearPorcentaje(-0.4))
    }

    @Test
    fun `un porcentaje que redondea a cero no se enseña con signo`() {
        // 1001 frente a 1000 es +0,1 %: redondea a 0 y no sube ni baja.
        val comparativa = calcularComparativa(listOf(actual(1001.0), cerrada(2024, 1000.0)))!!

        assertEquals(0, comparativa.variacion)
        assertNull(comparativa.sube)
        assertEquals("0 %", comparativa.textoVariacion)
    }

    @Test
    fun `la etiqueta corta acorta el nombre de la campana`() {
        assertEquals("24/25", etiquetaCorta("2024/2025"))
        assertEquals("Campaña rara", etiquetaCorta("Campaña rara"))
        assertEquals("2024-2025", etiquetaCorta("2024-2025"))
    }

    @Test
    fun `las barras van de la mas antigua a la actual y omiten las que no tienen cifra`() {
        val barras = barrasDeAceite(
            listOf(actual(1774.0), cerrada(2024, 1198.0), cerrada(2022, null), cerrada(2023, 900.0))
        )

        assertEquals(listOf("2023/2024", "2024/2025", "2025/2026"), barras.map { it.campana })
        assertEquals(listOf(false, false, true), barras.map { it.esActual })
        assertEquals(1774.0, barras.last().aceiteKg, 0.0)
    }

    @Test
    fun `con una sola campana el grafico no se enseña`() {
        assertTrue(barrasDeAceite(listOf(actual(1774.0))).isEmpty())
        assertTrue(barrasDeAceite(listOf(actual(1774.0), cerrada(2024, null))).isEmpty())
        assertNotNull(barrasDeAceite(listOf(actual(1774.0), cerrada(2024, 0.0))).firstOrNull())
    }
}
