package com.olivapp.app.simulador.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Formato español de importes y precios del simulador avanzado. */
class FormatoImportesTest {

    @Test
    fun `los euros llevan miles con punto y dos decimales con coma`() {
        assertEquals("1.234,50 €", formatearEuros(1234.5))
        assertEquals("765,00 €", formatearEuros(765.0))
        assertEquals("-5,00 €", formatearEuros(-5.0))
    }

    @Test
    fun `el precio por kilo usa dos decimales si bastan y tres si no`() {
        assertEquals("4,45 €/kg", formatearEurosPorKilo(4.45))
        assertEquals("3,855 €/kg", formatearEurosPorKilo(3.855))
        assertEquals("0,765 €/kg", formatearEurosPorKilo(0.765, decimalesMinimos = 3))
    }
}
