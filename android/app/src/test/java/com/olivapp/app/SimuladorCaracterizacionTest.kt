package com.olivapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests de caracterización del simulador de liquidación en limpio (features
 * calculo-de-liquidacion-en-limpio y ajuste-industrial-configurable).
 *
 * Fijan el comportamiento verificado a mano en el móvil antes del refactor de
 * arquitectura: los mismos ejemplos de las evidencias de esas features.
 */
class SimuladorCaracterizacionTest {

    private val delta = 1e-9

    @Test
    fun `1000 kg al 20 por ciento con el ajuste por defecto del 10 por ciento da 180 kg netos`() {
        val r = simularLiquidacion(kilos = 1000.0, rendimiento = 20.0)

        assertEquals(200.0, r.aceiteSegunRendimiento, delta)
        assertEquals(10.0, r.ajustePorcentaje, delta)
        assertEquals(20.0, r.ajusteIndustrial, delta)
        assertEquals(180.0, r.aceiteNeto, delta)
    }

    @Test
    fun `el ajuste al 5 por ciento da 190 kg netos`() {
        val r = simularLiquidacion(kilos = 1000.0, rendimiento = 20.0, ajustePorcentaje = 5.0)

        assertEquals(200.0, r.aceiteSegunRendimiento, delta)
        assertEquals(10.0, r.ajusteIndustrial, delta)
        assertEquals(190.0, r.aceiteNeto, delta)
    }

    @Test
    fun `2650,5 kg al 22 por ciento con ajuste al 10 por ciento cuadra a mano`() {
        val r = simularLiquidacion(kilos = 2650.5, rendimiento = 22.0)

        assertEquals(583.11, r.aceiteSegunRendimiento, delta)
        assertEquals(58.311, r.ajusteIndustrial, delta)
        assertEquals(524.799, r.aceiteNeto, delta)
    }

    @Test
    fun `10000 kg al 20 por ciento con ajuste al 5,5 por ciento da 1890 kg netos`() {
        val r = simularLiquidacion(kilos = 10000.0, rendimiento = 20.0, ajustePorcentaje = 5.5)

        assertEquals(2000.0, r.aceiteSegunRendimiento, delta)
        assertEquals(110.0, r.ajusteIndustrial, delta)
        assertEquals(1890.0, r.aceiteNeto, delta)
    }

    @Test
    fun `recalcular con ajuste solo cambia la deduccion, no el aceite segun rendimiento`() {
        val anterior = simularLiquidacion(kilos = 1000.0, rendimiento = 20.0)

        val r = recalcularConAjuste(anterior, ajustePorcentaje = 5.0)

        assertEquals(200.0, r.aceiteSegunRendimiento, delta)
        assertEquals(5.0, r.ajustePorcentaje, delta)
        assertEquals(10.0, r.ajusteIndustrial, delta)
        assertEquals(190.0, r.aceiteNeto, delta)
    }

    @Test
    fun `el campo numerico acepta coma y punto decimal`() {
        assertEquals(20.5, numeroDeCampo("20,5")!!, delta)
        assertEquals(20.5, numeroDeCampo("20.5")!!, delta)
        assertEquals(2650.5, numeroDeCampo(" 2650,5 ")!!, delta)
        assertEquals(850.0, numeroDeCampo("850")!!, delta)
    }

    @Test
    fun `el campo numerico acepta puntos de millar`() {
        assertEquals(10000.0, numeroDeCampo("10.000")!!, delta)
        assertEquals(1234.5, numeroDeCampo("1.234,5")!!, delta)
    }

    @Test
    fun `el campo numerico vacio o ilegible no da numero`() {
        assertNull(numeroDeCampo(""))
        assertNull(numeroDeCampo("   "))
        assertNull(numeroDeCampo("abc"))
    }
}
