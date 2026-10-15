package com.olivapp.app.core.ui

import com.olivapp.app.simulador.domain.formatearNumeroDeCampo
import org.junit.Assert.assertEquals
import org.junit.Test

/** El cursor no se queda atrás al insertarse el punto de millar (fallo visto en el móvil). */
class PosicionTrasFormatoTest {

    /** Teclea carácter a carácter al final, como en el móvil, con el formato del simulador. */
    private fun teclear(digitos: String): String {
        var texto = ""
        var cursor = 0
        for (c in digitos) {
            val nuevo = texto.substring(0, cursor) + c + texto.substring(cursor)
            val formateado = formatearNumeroDeCampo(nuevo)
            cursor = posicionTrasFormato(nuevo, cursor + 1, formateado)
            texto = formateado
        }
        return texto
    }

    @Test
    fun `teclear 10958 da 10_958 y no 10_985`() {
        assertEquals("10.958", teclear("10958"))
    }

    @Test
    fun `teclear un millon con decimales mantiene el orden`() {
        assertEquals("1.234.567", teclear("1234567"))
    }

    @Test
    fun `al teclear al final el cursor queda al final`() {
        assertEquals(5, posicionTrasFormato("1.095", 5, "1.095"))
        // Se acaba de insertar el punto de millar: el cursor sigue al final, tras el punto.
        assertEquals(5, posicionTrasFormato("1095", 4, "1.095"))
    }

    @Test
    fun `editar en medio conserva los digitos a la izquierda del cursor`() {
        // "10.958" con "7" tecleado tras el "10": nuevo "107.958" cursor 3 -> formateado "107.958".
        val nuevo = "107.958"
        val formateado = formatearNumeroDeCampo(nuevo)
        assertEquals("107.958", formateado)
        assertEquals(3, posicionTrasFormato(nuevo, 3, formateado))
    }

    @Test
    fun `cursor al inicio se queda al inicio`() {
        assertEquals(0, posicionTrasFormato("a1.095", 0, "1.095"))
    }
}
