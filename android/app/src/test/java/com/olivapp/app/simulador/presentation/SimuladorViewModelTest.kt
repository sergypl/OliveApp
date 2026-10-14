package com.olivapp.app.simulador.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del ViewModel del simulador de liquidación en limpio (features
 * calculo-de-liquidacion-en-limpio y ajuste-industrial-configurable). Puro
 * JVM: el cálculo no toca Android.
 */
class SimuladorViewModelTest {

    private val delta = 1e-9

    private fun conDatos() = SimuladorViewModel().apply {
        cambiarKilos("1000")
        cambiarRendimiento("20")
    }

    @Test
    fun `arranca con el ajuste por defecto del 10 por ciento y sin resultado`() {
        val viewModel = SimuladorViewModel()

        val estado = viewModel.uiState.value
        assertEquals("10", estado.ajuste)
        assertNull(estado.resultado)
        assertEquals("", estado.error)
    }

    @Test
    fun `simular calcula el aceite neto`() {
        val viewModel = conDatos()

        viewModel.simular()

        val resultado = viewModel.uiState.value.resultado!!
        assertEquals(200.0, resultado.aceiteSegunRendimiento, delta)
        assertEquals(10.0, resultado.ajustePorcentaje, delta)
        assertEquals(20.0, resultado.ajusteIndustrial, delta)
        assertEquals(180.0, resultado.aceiteNeto, delta)
        assertEquals("", viewModel.uiState.value.error)
    }

    @Test
    fun `los campos aceptan coma decimal y puntos de millar`() {
        val viewModel = SimuladorViewModel().apply {
            cambiarKilos("10.958,5")
            cambiarRendimiento("20")
            cambiarAjuste("10")
        }

        viewModel.simular()

        val resultado = viewModel.uiState.value.resultado!!
        assertEquals(2191.7, resultado.aceiteSegunRendimiento, delta)
    }

    @Test
    fun `simular sin kilos mayores que cero avisa`() {
        val viewModel = SimuladorViewModel().apply { cambiarRendimiento("20") }

        viewModel.simular()

        val estado = viewModel.uiState.value
        assertNull(estado.resultado)
        assertEquals("Introduce los kilos de aceituna, mayores que cero.", estado.error)
    }

    @Test
    fun `simular con rendimiento fuera de rango avisa`() {
        val viewModel = SimuladorViewModel().apply {
            cambiarKilos("1000")
            cambiarRendimiento("120")
        }

        viewModel.simular()

        val estado = viewModel.uiState.value
        assertNull(estado.resultado)
        assertEquals(
            "Introduce el rendimiento graso como porcentaje entre 0 y 100 (por ejemplo, 20).",
            estado.error
        )
    }

    @Test
    fun `simular con ajuste fuera de rango avisa`() {
        val viewModel = conDatos().apply { cambiarAjuste("-1") }

        viewModel.simular()

        val estado = viewModel.uiState.value
        assertNull(estado.resultado)
        assertEquals(
            "Introduce el ajuste industrial como porcentaje entre 0 y 100 (por ejemplo, 10).",
            estado.error
        )
    }

    @Test
    fun `cambiar kilos o rendimiento borra el resultado`() {
        val viewModel = conDatos()
        viewModel.simular()
        assertNotNull(viewModel.uiState.value.resultado)

        viewModel.cambiarKilos("2000")
        assertNull(viewModel.uiState.value.resultado)

        viewModel.simular()
        assertNotNull(viewModel.uiState.value.resultado)
        viewModel.cambiarRendimiento("18")
        assertNull(viewModel.uiState.value.resultado)
    }

    @Test
    fun `cambiar el ajuste recalcula el resultado al instante`() {
        val viewModel = conDatos()
        viewModel.simular()

        viewModel.cambiarAjuste("5")

        val estado = viewModel.uiState.value
        assertEquals("5", estado.ajuste)
        val resultado = estado.resultado!!
        // El aceite según el rendimiento no cambia: solo cambia la deducción.
        assertEquals(200.0, resultado.aceiteSegunRendimiento, delta)
        assertEquals(5.0, resultado.ajustePorcentaje, delta)
        assertEquals(190.0, resultado.aceiteNeto, delta)
    }

    @Test
    fun `un ajuste ilegible no recalcula ni borra el resultado`() {
        val viewModel = conDatos()
        viewModel.simular()

        viewModel.cambiarAjuste("abc")

        val estado = viewModel.uiState.value
        assertEquals("abc", estado.ajuste)
        assertEquals(180.0, estado.resultado!!.aceiteNeto, delta)
    }

    @Test
    fun `sin resultado, cambiar el ajuste no calcula nada`() {
        val viewModel = conDatos()

        viewModel.cambiarAjuste("5")

        assertNull(viewModel.uiState.value.resultado)
        assertTrue(viewModel.uiState.value.error.isEmpty())
    }
}
