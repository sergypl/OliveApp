package com.olivapp.app.campana.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests del ViewModel del panel (feature panel-de-campana). */
class PanelViewModelTest {

    @Test
    fun `con sesion abierta se cargan los datos oficiales y las campanas`() {
        val sesion = FakeSesionRepositorio()
        sesion.guardar("token-abc")
        val campanas = FakeCampanasRepositorio()
        val viewModel = PanelViewModel(sesion, campanas)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("token-abc", campanas.tokenLlamada)
        assertEquals("2025/2026", estado.datos!!.campana)
        assertEquals("10.958 kg", estado.datos!!.kilosEntregados)
        assertEquals("17,93 %", estado.datos!!.rendimientoGraso)
        assertEquals("1.774 kg", estado.datos!!.aceiteObtenido)
        assertEquals(listOf("c-2024"), estado.campanas.map { it.id })
    }

    @Test
    fun `sin token se avisa de que la sesion ha caducado`() {
        val campanas = FakeCampanasRepositorio()
        val viewModel = PanelViewModel(FakeSesionRepositorio(), campanas)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
        assertEquals(null, estado.datos)
        assertEquals(null, campanas.tokenLlamada)
    }

    @Test
    fun `si fallan las campanas se ensena el mensaje del backend`() {
        val sesion = FakeSesionRepositorio()
        sesion.guardar("token-abc")
        val campanas = FakeCampanasRepositorio()
        campanas.resultadoCampanas = null to "No se pudieron cargar tus campañas."
        val viewModel = PanelViewModel(sesion, campanas)

        viewModel.cargar()

        assertEquals("No se pudieron cargar tus campañas.", viewModel.uiState.value.mensaje)
    }

    @Test
    fun `cerrar sesion borra el token y lo avisa una sola vez`() {
        val sesion = FakeSesionRepositorio()
        sesion.guardar("token-abc")
        val viewModel = PanelViewModel(sesion, FakeCampanasRepositorio())

        viewModel.cerrarSesion()

        assertEquals(null, sesion.token())
        assertTrue(viewModel.uiState.value.sesionCerrada)

        viewModel.sesionCerradaAtendida()

        assertFalse(viewModel.uiState.value.sesionCerrada)
    }
}
