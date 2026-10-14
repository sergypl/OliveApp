package com.olivapp.app.parcelas.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests del ViewModel de parcelas (feature gestion-de-parcelas). */
class ParcelasViewModelTest {

    private fun sesionAbierta() = FakeSesionRepositorio().apply { guardar("token-abc") }

    @Test
    fun `al entrar se cargan las parcelas del socio`() {
        val repositorio = FakeParcelasRepositorio()
        val viewModel = ParcelasViewModel(sesionAbierta(), repositorio)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("token-abc", repositorio.tokenLlamada)
        assertEquals(listOf("Cañada Honda"), estado.parcelas.map { it.nombre })
    }

    @Test
    fun `sin sesion se avisa de que ha caducado`() {
        val repositorio = FakeParcelasRepositorio()
        val viewModel = ParcelasViewModel(FakeSesionRepositorio(), repositorio)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
        assertEquals(null, repositorio.tokenLlamada)
    }

    @Test
    fun `crear parcela exitosa ensena el aviso limpia el nombre y recarga`() {
        val repositorio = FakeParcelasRepositorio()
        val viewModel = ParcelasViewModel(sesionAbierta(), repositorio)

        viewModel.cambiarNombre("La Noria")
        viewModel.crearParcela()

        val estado = viewModel.uiState.value
        assertEquals("La Noria", repositorio.nombreLlamada)
        assertTrue(estado.creada)
        assertEquals("Parcela creada.", estado.mensaje)
        assertEquals("", estado.nombre)
        assertFalse(estado.trabajando)
        assertEquals(listOf("Cañada Honda"), estado.parcelas.map { it.nombre })
    }

    @Test
    fun `crear parcela fallida ensena el mensaje del backend`() {
        val repositorio = FakeParcelasRepositorio()
        repositorio.resultadoCrear = false to "No se pudo crear la parcela."
        val viewModel = ParcelasViewModel(sesionAbierta(), repositorio)

        viewModel.cambiarNombre("La Noria")
        viewModel.crearParcela()

        val estado = viewModel.uiState.value
        assertFalse(estado.creada)
        assertEquals("No se pudo crear la parcela.", estado.mensaje)
        assertEquals("La Noria", estado.nombre)
    }

    @Test
    fun `mientras el backend no responde se esta creando la parcela`() {
        val repositorio = FakeParcelasRepositorio(responderAlMomento = false)
        val viewModel = ParcelasViewModel(sesionAbierta(), repositorio)

        viewModel.crearParcela()

        assertTrue(viewModel.uiState.value.trabajando)

        repositorio.pendienteCrear!!.invoke(true, "Parcela creada.")

        assertFalse(viewModel.uiState.value.trabajando)
        assertTrue(viewModel.uiState.value.creada)
    }
}
