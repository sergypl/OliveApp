package com.olivapp.app.campana.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests del saludo del panel según la hora (feature panel-moderno). */
class SaludoTest {

    @Test
    fun `por la manana y hasta la comida es buenos dias`() {
        for (hora in 6..13) assertEquals("hora $hora", "Buenos días", saludoSegunHora(hora))
    }

    @Test
    fun `desde las 14 y hasta las 20 es buenas tardes`() {
        for (hora in 14..20) assertEquals("hora $hora", "Buenas tardes", saludoSegunHora(hora))
    }

    @Test
    fun `de noche y de madrugada es buenas noches`() {
        for (hora in listOf(21, 22, 23, 0, 1, 5)) {
            assertEquals("hora $hora", "Buenas noches", saludoSegunHora(hora))
        }
    }

    @Test
    fun `los limites cambian justo en las 6 las 14 y las 21`() {
        assertEquals("Buenas noches", saludoSegunHora(5))
        assertEquals("Buenos días", saludoSegunHora(6))
        assertEquals("Buenos días", saludoSegunHora(13))
        assertEquals("Buenas tardes", saludoSegunHora(14))
        assertEquals("Buenas tardes", saludoSegunHora(20))
        assertEquals("Buenas noches", saludoSegunHora(21))
    }

    @Test
    fun `la cabecera suma el codigo de socio si se conoce`() {
        assertEquals("Buenos días, Socio 1", saludoDeCabecera(9, "Socio 1"))
        assertEquals("Buenas tardes", saludoDeCabecera(16, null))
        assertEquals("Buenas noches", saludoDeCabecera(23, "  "))
    }
}
