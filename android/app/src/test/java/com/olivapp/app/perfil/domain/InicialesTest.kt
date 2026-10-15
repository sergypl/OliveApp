package com.olivapp.app.perfil.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Iniciales del avatar del perfil (feature perfil-del-socio). */
class InicialesTest {

    @Test
    fun `dos palabras en el email dan las dos iniciales`() {
        assertEquals("JP", inicialesDe("juan.perez@ejemplo.com"))
        assertEquals("MG", inicialesDe("maria_garcia@ejemplo.com"))
        assertEquals("AL", inicialesDe("ana-lopez@ejemplo.com"))
    }

    @Test
    fun `una sola palabra da sus dos primeras letras`() {
        assertEquals("SO", inicialesDe("socio@ejemplo.com"))
    }

    @Test
    fun `los numeros no cuentan como iniciales`() {
        assertEquals("SO", inicialesDe("socio1@ejemplo.com"))
        assertEquals("JP", inicialesDe("juan.perez99@ejemplo.com"))
    }

    @Test
    fun `sin letras sale un interrogante`() {
        assertEquals("?", inicialesDe("12345@ejemplo.com"))
        assertEquals("?", inicialesDe(""))
    }
}
