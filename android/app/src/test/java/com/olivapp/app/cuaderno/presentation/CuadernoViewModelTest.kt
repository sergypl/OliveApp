package com.olivapp.app.cuaderno.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import com.olivapp.app.cuaderno.domain.DatosCampanaDeLabores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Con Robolectric como el resto de la batería: los ViewModels construyen
// JSON (org.json), que en JVM sin Robolectric no está implementado.
@RunWith(RobolectricTestRunner::class)
/** Tests del ViewModel de "Mi cuaderno" (feature consulta-cuaderno-por-campana). */
class CuadernoViewModelTest {

    private fun sesionAbierta() = FakeSesionRepositorio().apply { guardar("token-abc") }

    @Test
    fun `con sesion abierta se carga el cuaderno agrupado por campana`() {
        val labores = FakeLaboresRepositorio()
        labores.resultadoMisLabores = listOf(
            DatosCampanaDeLabores(
                nombre = "2025/2026",
                esActual = true,
                labores = listOf(datosLaborDeEjemplo())
            )
        ) to ""
        val viewModel = CuadernoViewModel(sesionAbierta(), labores)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("token-abc", labores.tokenLlamada)
        assertTrue(estado.cargado)
        assertEquals(listOf("2025/2026"), estado.campanas.map { it.nombre })
        assertEquals(listOf("lab-7"), estado.campanas[0].labores.map { it.editable.id })
    }

    @Test
    fun `sin sesion se avisa de que ha caducado y se deja de cargar`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = CuadernoViewModel(FakeSesionRepositorio(), labores)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
        assertTrue(estado.cargado)
        assertEquals(null, labores.tokenLlamada)
    }

    @Test
    fun `si falla la carga se ensena el mensaje del backend`() {
        val labores = FakeLaboresRepositorio()
        labores.resultadoMisLabores = null to "No se pudo cargar tu cuaderno."
        val viewModel = CuadernoViewModel(sesionAbierta(), labores)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("No se pudo cargar tu cuaderno.", estado.mensaje)
        assertTrue(estado.cargado)
    }

    @Test
    fun `pedir eliminar pide confirmacion y cancelar la retira`() {
        val viewModel = CuadernoViewModel(sesionAbierta(), FakeLaboresRepositorio())
        val labor = laborDeEjemplo()

        viewModel.pedirEliminar(labor)
        assertEquals("lab-7", viewModel.uiState.value.laborParaEliminar?.id)

        viewModel.cancelarEliminar()
        assertNull(viewModel.uiState.value.laborParaEliminar)
    }

    @Test
    fun `confirmar eliminar borra la labor y recarga el cuaderno`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = CuadernoViewModel(sesionAbierta(), labores)

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()

        val estado = viewModel.uiState.value
        assertEquals("lab-7", labores.idLlamado)
        assertEquals("Labor eliminada de tu cuaderno.", estado.mensaje)
        assertNull(estado.laborParaEliminar)
        // Tras el borrado se recarga: el fake vuelve a ser consultado.
        assertEquals("token-abc", labores.tokenLlamada)
    }

    @Test
    fun `confirmar eliminar sin sesion no borra nada y avisa`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = CuadernoViewModel(FakeSesionRepositorio(), labores)

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()

        val estado = viewModel.uiState.value
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
        assertNull(labores.idLlamado)
    }
}
