package com.olivapp.app.core.formatos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests de la variación de la tarjeta de dato de core/ui (feature
 * fundamentos-visuales-modernos): cálculo del porcentaje y su texto con signo
 * en formato español.
 */
class VariacionTest {

    @Test
    fun `la variacion sube respecto de la cifra anterior`() {
        val v = variacionPorcentual(actual = 1774.0, anterior = 1198.0)!!
        assertEquals(48.08, v, 0.01)
        assertEquals("+48,1 %", textoVariacion(v))
    }

    @Test
    fun `la variacion baja se muestra con signo menos y icono de bajada`() {
        val v = variacionPorcentual(actual = 100.0, anterior = 200.0)!!
        assertEquals(-50.0, v, 0.0)
        assertEquals("−50 %", textoVariacion(v))
    }

    @Test
    fun `la variacion entera no lleva decimales`() {
        assertEquals("+48 %", textoVariacion(48.0))
        assertEquals("+5 %", textoVariacion(5.0))
    }

    @Test
    fun `sin cambio la variacion es cero sin signo`() {
        val v = variacionPorcentual(actual = 500.0, anterior = 500.0)!!
        assertEquals(0.0, v, 0.0)
        assertEquals("0 %", textoVariacion(v))
    }

    @Test
    fun `sin cifra anterior no hay variacion que mostrar`() {
        assertNull(variacionPorcentual(actual = 500.0, anterior = 0.0))
        assertNull(variacionPorcentual(actual = 500.0, anterior = -1.0))
    }

    @Test
    fun `sin cifra actual no hay variacion que mostrar`() {
        assertNull(variacionPorcentual(actual = 0.0, anterior = 500.0))
    }
}
