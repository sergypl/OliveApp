package com.olivapp.app.simulador.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests del campo numérico del simulador: el formato español mientras se
 * teclea (miles con punto y coma decimal) y su lectura con `numeroDeCampo`
 * (feature simulador-calculo-en-vivo).
 */
class SimulacionTest {

    private val delta = 1e-9

    @Test
    fun `los millares se agrupan con punto al teclear`() {
        assertEquals("1.000", formatearNumeroDeCampo("1000"))
        assertEquals("10.000", formatearNumeroDeCampo("10000"))
        assertEquals("1.234.567", formatearNumeroDeCampo("1234567"))
        assertEquals("850", formatearNumeroDeCampo("850"))
    }

    @Test
    fun `la coma decimal se deja tal como se teclea`() {
        assertEquals("1.000,5", formatearNumeroDeCampo("1000,5"))
        assertEquals("20,5", formatearNumeroDeCampo("20,5"))
        assertEquals("1.234,5", formatearNumeroDeCampo("1234,5"))
    }

    @Test
    fun `el punto decimal tecleado se escribe con coma`() {
        assertEquals("20,5", formatearNumeroDeCampo("20.5"))
        assertEquals("20,55", formatearNumeroDeCampo("20.55"))
    }

    @Test
    fun `el punto con tres cifras detras es millar y con mas cifras suma al numero`() {
        assertEquals("1.234", formatearNumeroDeCampo("1.234"))
        assertEquals("10.000", formatearNumeroDeCampo("1.0000"))
    }

    @Test
    fun `un separador recien tecleado se conserva para seguir escribiendo`() {
        assertEquals("20,", formatearNumeroDeCampo("20,"))
        assertEquals("20,", formatearNumeroDeCampo("20."))
    }

    @Test
    fun `los ceros a la izquierda no ensucian el numero`() {
        assertEquals("0", formatearNumeroDeCampo("0"))
        assertEquals("7", formatearNumeroDeCampo("007"))
        assertEquals("0,5", formatearNumeroDeCampo("0,5"))
    }

    @Test
    fun `lo ilegible se deja tal cual, sin formatear`() {
        assertEquals("", formatearNumeroDeCampo(""))
        assertEquals("abc", formatearNumeroDeCampo("abc"))
        assertEquals("12a", formatearNumeroDeCampo("12a"))
    }

    @Test
    fun `lo formateado se vuelve a leer con el mismo valor`() {
        assertEquals(1000.0, numeroDeCampo(formatearNumeroDeCampo("1000"))!!, delta)
        assertEquals(1000.5, numeroDeCampo(formatearNumeroDeCampo("1000,5"))!!, delta)
        assertEquals(1234.5, numeroDeCampo(formatearNumeroDeCampo("1234.5"))!!, delta)
        assertNull(numeroDeCampo(formatearNumeroDeCampo("")))
    }
}
