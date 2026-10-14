package com.olivapp.app.campana.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests del ViewModel del detalle de campaña (feature detalle-de-campana). */
class CampanaViewModelTest {

    @Test
    fun `con sesion abierta se carga la campana pedida`() {
        val sesion = FakeSesionRepositorio()
        sesion.guardar("token-abc")
        val campanas = FakeCampanasRepositorio()
        val viewModel = CampanaViewModel(sesion, campanas)

        viewModel.cargar("c-2024")

        val estado = viewModel.uiState.value
        assertEquals("token-abc", campanas.tokenLlamada)
        assertEquals("c-2024", campanas.campanaIdLlamada)
        assertEquals("2024/2025", estado.datos!!.nombre)
        assertEquals("cerrada", estado.datos!!.estado)
        // El código original guardaba el mensaje del callback tal cual: con
        // éxito queda vacío (no null).
        assertEquals("", estado.mensaje)
    }

    @Test
    fun `sin token se avisa de que la sesion ha caducado`() {
        val campanas = FakeCampanasRepositorio()
        val viewModel = CampanaViewModel(FakeSesionRepositorio(), campanas)

        viewModel.cargar("c-2024")

        val estado = viewModel.uiState.value
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
        assertNull(estado.datos)
    }

    @Test
    fun `si falla la carga se ensena el mensaje del backend`() {
        val sesion = FakeSesionRepositorio()
        sesion.guardar("token-abc")
        val campanas = FakeCampanasRepositorio()
        campanas.resultadoCampana = null to "No se pudo cargar la campaña."
        val viewModel = CampanaViewModel(sesion, campanas)

        viewModel.cargar("c-2024")

        assertEquals("No se pudo cargar la campaña.", viewModel.uiState.value.mensaje)
        assertNull(viewModel.uiState.value.datos)
    }
}
