package com.olivapp.app.simulador.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de las funciones puras del simulador avanzado (feature
 * simulador-avanzado-precio-aceite): precio ponderado, importes y campañas.
 */
class ImporteAvanzadoTest {

    private val delta = 1e-9
    private val precios = mapOf(
        CategoriaAceite.AOVE to 4.0,
        CategoriaAceite.VIRGEN to 3.5,
        CategoriaAceite.LAMPANTE to 3.0
    )

    @Test
    fun `por defecto el reparto es el 100 por cien de la categoria elegida`() {
        val reparto = RepartoCalidad.completo(CategoriaAceite.VIRGEN)

        assertEquals(0.0, reparto.aove, delta)
        assertEquals(100.0, reparto.virgen, delta)
        assertEquals(0.0, reparto.lampante, delta)
        assertTrue(reparto.sumaCien)
        assertEquals(3.5, precioPonderado(precios, reparto)!!, delta)
    }

    @Test
    fun `el precio ponderado reparte por calidad con los precios de la misma semana`() {
        val reparto = RepartoCalidad(aove = 60.0, virgen = 30.0, lampante = 10.0)

        // 4,0·0,6 + 3,5·0,3 + 3,0·0,1 = 2,4 + 1,05 + 0,3
        assertEquals(3.75, precioPonderado(precios, reparto)!!, delta)
    }

    @Test
    fun `un reparto que no suma 100 no da precio ponderado`() {
        val corto = RepartoCalidad(aove = 60.0, virgen = 30.0, lampante = 0.0)
        val largo = RepartoCalidad(aove = 60.0, virgen = 30.0, lampante = 20.0)

        assertFalse(corto.sumaCien)
        assertEquals(90.0, corto.suma, delta)
        assertNull(precioPonderado(precios, corto))
        assertNull(precioPonderado(precios, largo))
    }

    @Test
    fun `el reparto con decimales que suma 100 es valido`() {
        val reparto = RepartoCalidad(aove = 33.3, virgen = 33.3, lampante = 33.4)

        assertTrue(reparto.sumaCien)
        assertNotNull(precioPonderado(precios, reparto))
    }

    @Test
    fun `sin el precio de una categoria con porcentaje no se inventa el ponderado`() {
        val sinLampante = precios - CategoriaAceite.LAMPANTE

        assertNull(precioPonderado(sinLampante, RepartoCalidad(50.0, 30.0, 20.0)))
        // Con 0 % de lampante, su precio no hace falta.
        assertEquals(
            3.85,
            precioPonderado(sinLampante, RepartoCalidad(70.0, 30.0, 0.0))!!,
            delta
        )
    }

    @Test
    fun `importe bruto, gastos, neto y euros por kilo de aceituna`() {
        // 1.000 kg de aceituna al 20 % con ajuste del 10 % = 180 kg de aceite neto.
        val resultado = calcularImporteAvanzado(
            aceiteNeto = 180.0,
            kilosDeAceituna = 1000.0,
            precio = 4.45,
            gastosPorKilo = 0.20
        )!!

        assertEquals(801.0, resultado.importeBruto, delta) // 180 · 4,45
        assertEquals(36.0, resultado.gastos, delta) // 180 · 0,20
        assertEquals(765.0, resultado.importeNeto, delta)
        assertEquals(0.765, resultado.netoPorKiloDeAceituna, delta)
        assertFalse(resultado.gastosSuperanElPrecio)
    }

    @Test
    fun `gastos a cero no restan nada`() {
        val resultado = calcularImporteAvanzado(180.0, 1000.0, 4.0, 0.0)!!

        assertEquals(720.0, resultado.importeBruto, delta)
        assertEquals(0.0, resultado.gastos, delta)
        assertEquals(720.0, resultado.importeNeto, delta)
    }

    @Test
    fun `precio cero no es un precio y no calcula importe`() {
        assertNull(calcularImporteAvanzado(180.0, 1000.0, 0.0, 0.20))
        assertNull(calcularImporteAvanzado(180.0, 1000.0, -1.0, 0.20))
    }

    @Test
    fun `gastos mayores que el precio dan un neto negativo y se marcan`() {
        val resultado = calcularImporteAvanzado(100.0, 500.0, 0.15, 0.20)!!

        assertEquals(15.0, resultado.importeBruto, delta)
        assertEquals(20.0, resultado.gastos, delta)
        assertEquals(-5.0, resultado.importeNeto, delta)
        assertEquals(-0.01, resultado.netoPorKiloDeAceituna, delta)
        assertTrue(resultado.gastosSuperanElPrecio)
    }

    @Test
    fun `sin kilos ni aceite neto o con gastos negativos no hay importe`() {
        assertNull(calcularImporteAvanzado(0.0, 1000.0, 4.0, 0.20))
        assertNull(calcularImporteAvanzado(180.0, 0.0, 4.0, 0.20))
        assertNull(calcularImporteAvanzado(180.0, 1000.0, 4.0, -0.10))
    }

    @Test
    fun `las campanas disponibles van de la mas reciente a 2010-2011`() {
        // En octubre de 2026 la campaña en curso es 2026/2027.
        val octubre = campanasDisponibles(anio = 2026, mes = 10)
        assertEquals("2026/2027", octubre.first())
        assertEquals("2025/2026", octubre[1])
        assertEquals("2010/2011", octubre.last())
        assertEquals(17, octubre.size)

        // En marzo de 2026 todavía es la 2025/2026.
        assertEquals("2025/2026", campanasDisponibles(anio = 2026, mes = 3).first())
        // Septiembre ya ofrece la campaña nueva (semanas de cambio).
        assertEquals("2026/2027", campanasDisponibles(anio = 2026, mes = 9).first())
    }
}
