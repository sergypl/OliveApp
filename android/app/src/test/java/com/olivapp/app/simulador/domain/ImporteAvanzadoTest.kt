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
    fun `ejemplo a mano con el precio real de la media nacional del 28 de septiembre de 2026`() {
        // 12.000 kg al 20 % = 2.400 kg; ajuste industrial del 10 % = 2.160 kg de aceite neto.
        val neto = simularLiquidacion(12000.0, 20.0).aceiteNeto
        assertEquals(2160.0, neto, delta)

        // AOVE, media nacional: 343,19 €/100 kg = 3,4319 €/kg.
        val resultado = calcularImporteAvanzado(neto, 12000.0, 3.4319, GASTOS_POR_DEFECTO)!!
        assertEquals(7412.904, resultado.importeBruto, 1e-6) // 2.160 · 3,4319
        assertEquals(432.0, resultado.gastos, 1e-6) // 2.160 · 0,20
        assertEquals(6980.904, resultado.importeNeto, 1e-6)
        assertEquals(0.58174, resultado.netoPorKiloDeAceituna, 1e-5)
        assertEquals("7.412,90 €", formatearEuros(resultado.importeBruto))
        assertEquals("6.980,90 €", formatearEuros(resultado.importeNeto))
        assertEquals("0,582 €/kg", formatearEurosPorKilo(resultado.netoPorKiloDeAceituna, 3))

        // Con reparto 60/30/10 y los precios de esa misma semana (3,4319 / 3,1757 / 3,0229).
        val ponderado = precioPonderado(
            mapOf(
                CategoriaAceite.AOVE to 3.4319,
                CategoriaAceite.VIRGEN to 3.1757,
                CategoriaAceite.LAMPANTE to 3.0229
            ),
            RepartoCalidad(60.0, 30.0, 10.0)
        )!!
        assertEquals(3.31414, ponderado, 1e-9)
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
