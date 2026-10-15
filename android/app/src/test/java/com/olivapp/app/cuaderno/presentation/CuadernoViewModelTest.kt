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
/** Tests del ViewModel de "Mi cuaderno" (features consulta-cuaderno-por-campana y
 *  cuaderno-linea-de-tiempo). */
class CuadernoViewModelTest {

    private fun sesionAbierta() = FakeSesionRepositorio().apply { guardar("token-abc") }

    /** Un cuaderno ya cargado con una labor en la campaña actual. */
    private fun viewModelConUnaLabor(
        labores: FakeLaboresRepositorio,
        sesion: FakeSesionRepositorio = sesionAbierta()
    ): CuadernoViewModel {
        labores.resultadoMisLabores = listOf(
            DatosCampanaDeLabores(
                nombre = "2025/2026",
                esActual = true,
                labores = listOf(datosLaborDeEjemplo())
            )
        ) to ""
        val viewModel = CuadernoViewModel(sesion, labores)
        viewModel.cargar()
        return viewModel
    }

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
    fun `confirmar eliminar quita la labor de la lista pero todavia no la borra`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = viewModelConUnaLabor(labores)

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()

        val estado = viewModel.uiState.value
        // El backend no se entera hasta que se cierre el mensaje sin deshacer.
        assertNull(labores.idLlamado)
        assertTrue(estado.campanas[0].labores.isEmpty())
        assertEquals("lab-7", estado.laborBorrada?.id)
    }

    @Test
    fun `si no se deshace, al cerrarse el mensaje la labor se borra y se recarga`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = viewModelConUnaLabor(labores)

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()
        viewModel.cerrarAvisoDeBorrado()

        val estado = viewModel.uiState.value
        assertEquals("lab-7", labores.idLlamado)
        assertNull(estado.laborBorrada)
        // Tras el borrado se recarga: el fake vuelve a ser consultado.
        assertEquals("token-abc", labores.tokenLlamada)
    }

    @Test
    fun `al deshacer la labor vuelve a su sitio y no se borra nada`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = viewModelConUnaLabor(labores)

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()
        viewModel.deshacerEliminar()

        val estado = viewModel.uiState.value
        assertNull(labores.idLlamado)
        assertNull(estado.laborBorrada)
        assertEquals(listOf("lab-7"), estado.campanas[0].labores.map { it.editable.id })
    }

    @Test
    fun `al salir con el mensaje abierto la labor se borra`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = viewModelConUnaLabor(labores)

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()
        viewModel.alSalir()

        assertEquals("lab-7", labores.idLlamado)
    }

    @Test
    fun `al girar la pantalla no se confirma el borrado y deshacer sigue funcionando`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = viewModelConUnaLabor(labores)

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()
        // La rotación no es salir de la pantalla: el borrado sigue diferido.
        viewModel.alSalir(cambiandoLaConfiguracion = true)

        assertNull(labores.idLlamado)
        assertEquals("lab-7", viewModel.uiState.value.laborBorrada?.id)

        viewModel.deshacerEliminar()

        val estado = viewModel.uiState.value
        assertNull(estado.laborBorrada)
        assertEquals(listOf("lab-7"), estado.campanas[0].labores.map { it.editable.id })
    }

    @Test
    fun `al salir de verdad tras una rotacion el borrado pendiente se confirma`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = viewModelConUnaLabor(labores)

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()
        viewModel.alSalir(cambiandoLaConfiguracion = true)
        viewModel.alSalir()

        assertEquals("lab-7", labores.idLlamado)
    }

    @Test
    fun `si el borrado falla la labor vuelve a la lista y se avisa`() {
        val labores = FakeLaboresRepositorio()
        labores.resultadoEliminar = false to "No se pudo eliminar la labor."
        val viewModel = viewModelConUnaLabor(labores)

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()
        viewModel.cerrarAvisoDeBorrado()

        val estado = viewModel.uiState.value
        assertEquals("No se pudo eliminar la labor. Vuelve a estar en tu cuaderno.", estado.mensaje)
        assertEquals(listOf("lab-7"), estado.campanas[0].labores.map { it.editable.id })
    }

    @Test
    fun `confirmar eliminar sin sesion no borra nada y avisa`() {
        val labores = FakeLaboresRepositorio()
        val sesion = sesionAbierta()
        val viewModel = viewModelConUnaLabor(labores, sesion)
        sesion.borrar()

        viewModel.pedirEliminar(laborDeEjemplo())
        viewModel.confirmarEliminar()
        viewModel.cerrarAvisoDeBorrado()

        val estado = viewModel.uiState.value
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
        assertNull(labores.idLlamado)
    }

    @Test
    fun `los filtros de categoria y parcela se guardan en el estado`() {
        val viewModel = CuadernoViewModel(sesionAbierta(), FakeLaboresRepositorio())

        viewModel.filtrarPorCategoria("riego")
        viewModel.filtrarPorParcela("La Noria")

        val filtro = viewModel.uiState.value.filtro
        assertEquals("riego", filtro.categoria)
        assertEquals("La Noria", filtro.parcela)

        viewModel.filtrarPorCategoria(null)
        viewModel.filtrarPorParcela(null)
        assertTrue(viewModel.uiState.value.filtro.sinFiltros)
    }
}
