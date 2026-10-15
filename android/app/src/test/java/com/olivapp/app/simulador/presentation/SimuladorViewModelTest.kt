package com.olivapp.app.simulador.presentation

import com.olivapp.app.simulador.domain.numeroDeCampo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del ViewModel del simulador de liquidación en limpio (features
 * calculo-de-liquidacion-en-limpio, ajuste-industrial-configurable y
 * simulador-calculo-en-vivo). Puro JVM: el cálculo no toca Android.
 *
 * Desde simulador-calculo-en-vivo no hay botón "Simular": el resultado se
 * recalcula al teclear y los tests escriben los campos y miran el estado.
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
    fun `escribir kilos y rendimiento calcula el aceite neto al instante`() {
        val viewModel = conDatos()

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

        val resultado = viewModel.uiState.value.resultado!!
        assertEquals(2191.7, resultado.aceiteSegunRendimiento, delta)
    }

    @Test
    fun `los millares se formatean al escribir sin perder el valor`() {
        val viewModel = SimuladorViewModel()

        viewModel.cambiarKilos("1000")
        assertEquals("1.000", viewModel.uiState.value.kilos)

        // Seguir tecleando detrás del punto de millar suma cifras al número.
        viewModel.cambiarKilos("1.0000")
        assertEquals("10.000", viewModel.uiState.value.kilos)

        viewModel.cambiarKilos("1000,5")
        assertEquals("1.000,5", viewModel.uiState.value.kilos)
        assertEquals(1000.5, numeroDeCampo(viewModel.uiState.value.kilos)!!, delta)
    }

    @Test
    fun `sin kilos se muestra el estado neutro, sin error`() {
        val viewModel = SimuladorViewModel().apply { cambiarRendimiento("20") }

        val estado = viewModel.uiState.value
        assertNull(estado.resultado)
        assertEquals("", estado.error)
        assertEquals("Escribe los kilos y el rendimiento", estado.neutro)
    }

    @Test
    fun `kilos en cero avisa`() {
        val viewModel = SimuladorViewModel().apply {
            cambiarKilos("0")
            cambiarRendimiento("20")
        }

        val estado = viewModel.uiState.value
        assertNull(estado.resultado)
        assertEquals("Introduce los kilos de aceituna, mayores que cero.", estado.error)
        assertEquals("", estado.neutro)
    }

    @Test
    fun `con rendimiento fuera de rango avisa`() {
        val viewModel = SimuladorViewModel().apply {
            cambiarKilos("1000")
            cambiarRendimiento("120")
        }

        val estado = viewModel.uiState.value
        assertNull(estado.resultado)
        assertEquals(
            "Introduce el rendimiento graso como porcentaje entre 0 y 100 (por ejemplo, 20).",
            estado.error
        )
    }

    @Test
    fun `con ajuste fuera de rango avisa`() {
        val viewModel = conDatos().apply { cambiarAjuste("-1") }

        val estado = viewModel.uiState.value
        assertNull(estado.resultado)
        assertEquals(
            "Introduce el ajuste industrial como porcentaje entre 0 y 100 (por ejemplo, 10).",
            estado.error
        )
    }

    @Test
    fun `cambiar kilos o rendimiento recalcula el resultado al instante`() {
        val viewModel = conDatos()
        assertEquals(180.0, viewModel.uiState.value.resultado!!.aceiteNeto, delta)

        viewModel.cambiarKilos("2000")
        assertEquals(360.0, viewModel.uiState.value.resultado!!.aceiteNeto, delta)

        viewModel.cambiarRendimiento("18")
        assertEquals(324.0, viewModel.uiState.value.resultado!!.aceiteNeto, delta)
    }

    @Test
    fun `cambiar el ajuste recalcula el resultado al instante`() {
        val viewModel = conDatos()

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
    fun `un ajuste ilegible deja el resultado con el 10 por ciento por defecto`() {
        val viewModel = conDatos()

        viewModel.cambiarAjuste("abc")

        val estado = viewModel.uiState.value
        assertEquals("abc", estado.ajuste)
        // El campo ilegible no da error: se calcula con el 10 % por defecto.
        assertEquals(10.0, estado.resultado!!.ajustePorcentaje, delta)
        assertEquals(180.0, estado.resultado!!.aceiteNeto, delta)
    }

    @Test
    fun `sin datos tecleados cambiar el ajuste no calcula nada`() {
        val viewModel = SimuladorViewModel()

        viewModel.cambiarAjuste("5")

        assertNull(viewModel.uiState.value.resultado)
        assertTrue(viewModel.uiState.value.error.isEmpty())
    }

    @Test
    fun `los botones de paso mueven el rendimiento 0,1 por ciento`() {
        val viewModel = SimuladorViewModel().apply { cambiarRendimiento("20") }

        viewModel.subirRendimiento()
        assertEquals("20,1", viewModel.uiState.value.rendimiento)

        viewModel.bajarRendimiento()
        assertEquals("20", viewModel.uiState.value.rendimiento)
    }

    @Test
    fun `los botones de paso respetan los limites del deslizador`() {
        val viewModel = SimuladorViewModel().apply { cambiarRendimiento("30") }

        viewModel.subirRendimiento()
        assertEquals("30", viewModel.uiState.value.rendimiento)

        viewModel.cambiarRendimiento("10")
        viewModel.bajarRendimiento()
        assertEquals("10", viewModel.uiState.value.rendimiento)
    }

    @Test
    fun `el deslizador sincroniza el rendimiento y recalcula`() {
        val viewModel = conDatos()

        viewModel.cambiarRendimientoConDeslizador(20.5)

        val estado = viewModel.uiState.value
        assertEquals("20,5", estado.rendimiento)
        assertEquals(184.5, estado.resultado!!.aceiteNeto, delta)
    }

    @Test
    fun `sin resultado la tarjeta muestra el estado neutro y no un error`() {
        val viewModel = SimuladorViewModel()

        val estado = viewModel.uiState.value
        assertNull(estado.resultado)
        assertTrue(estado.error.isEmpty())
        assertEquals("Escribe los kilos y el rendimiento", estado.neutro)
    }
}
