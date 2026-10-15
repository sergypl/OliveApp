package com.olivapp.app.voz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de la lógica de la grabación animada (feature grabacion-voz-animada). */
class GrabacionAnimadaTest {

    @Test
    fun `sin amplitud o con amplitud negativa el nivel es cero`() {
        assertEquals(0f, normalizarAmplitud(0), 0f)
        assertEquals(0f, normalizarAmplitud(-5), 0f)
    }

    @Test
    fun `la amplitud maxima da el nivel uno y no se pasa nunca`() {
        assertEquals(1f, normalizarAmplitud(AMPLITUD_MAXIMA), 0.0001f)
        assertEquals(1f, normalizarAmplitud(AMPLITUD_MAXIMA * 2), 0.0001f)
    }

    @Test
    fun `un susurro por debajo del umbral de silencio queda en cero`() {
        assertEquals(0f, normalizarAmplitud(50), 0f)
    }

    @Test
    fun `el nivel crece con la amplitud y la voz normal se nota`() {
        val bajo = normalizarAmplitud(500)
        val medio = normalizarAmplitud(4000)
        val alto = normalizarAmplitud(20000)

        assertTrue(bajo < medio && medio < alto)
        // Con escala lineal 4000/32767 daría 0,12: en decibelios se ve de verdad.
        assertTrue(medio > 0.4f)
        assertTrue(alto <= 1f)
    }

    @Test
    fun `la onda no guarda mas muestras que el maximo y descarta las mas antiguas`() {
        var onda = emptyList<Float>()
        for (i in 1..5) onda = agregarNivelDeOnda(onda, i / 10f, maximo = 3)

        assertEquals(listOf(0.3f, 0.4f, 0.5f), onda)
    }

    @Test
    fun `un nivel fuera de rango se recorta al añadirlo`() {
        val onda = agregarNivelDeOnda(emptyList(), 3f)

        assertEquals(listOf(1f), onda)
    }

    @Test
    fun `la onda en silencio tiene todas sus muestras a cero`() {
        val onda = ondaEnSilencio(4)

        assertEquals(listOf(0f, 0f, 0f, 0f), onda)
    }

    @Test
    fun `la cuenta atras baja con los segundos y nunca es negativa`() {
        assertEquals(60, segundosRestantes(0, 60))
        assertEquals(45, segundosRestantes(15, 60))
        assertEquals(0, segundosRestantes(60, 60))
        assertEquals(0, segundosRestantes(75, 60))
    }

    @Test
    fun `el anillo empieza lleno, se vacia con el tiempo y termina vacio`() {
        assertEquals(1f, fraccionRestante(0f, 60), 0.0001f)
        assertEquals(0.75f, fraccionRestante(15f, 60), 0.0001f)
        assertEquals(0f, fraccionRestante(60f, 60), 0.0001f)
        assertEquals(0f, fraccionRestante(90f, 60), 0.0001f)
    }

    @Test
    fun `un limite no valido no rompe el anillo`() {
        assertEquals(0f, fraccionRestante(5f, 0), 0f)
    }
}
