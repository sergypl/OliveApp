package com.olivapp.app.perfil.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests del ViewModel del perfil (feature perfil-del-socio). */
class PerfilViewModelTest {

    private fun sesionAbierta() = FakeSesionRepositorio().apply { guardar("token-abc") }

    @Test
    fun `al entrar se carga el perfil con el token guardado`() {
        val repositorio = FakePerfilRepositorio()
        val viewModel = PerfilViewModel(sesionAbierta(), repositorio)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("token-abc", repositorio.tokenLlamada)
        assertEquals("Socio 1", estado.perfil!!.codigoSocio)
        assertEquals("2025/2026", estado.perfil!!.campanaActual)
        assertEquals(4, estado.perfil!!.numeroParcelas)
        assertNull(estado.mensaje)
    }

    @Test
    fun `antes de cargar no hay perfil ni error (estado de carga)`() {
        val viewModel = PerfilViewModel(sesionAbierta(), FakePerfilRepositorio())

        val estado = viewModel.uiState.value

        assertNull(estado.perfil)
        assertNull(estado.mensaje)
    }

    @Test
    fun `sin sesion se avisa de que ha caducado y no se llama a la red`() {
        val repositorio = FakePerfilRepositorio()
        val viewModel = PerfilViewModel(FakeSesionRepositorio(), repositorio)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
        assertFalse(estado.sinConexion)
        assertEquals(0, repositorio.llamadas)
    }

    @Test
    fun `si falla el servidor se ensena su mensaje como error`() {
        val repositorio = FakePerfilRepositorio()
        repositorio.resultado = null to "No se pudo cargar tu perfil."
        val viewModel = PerfilViewModel(sesionAbierta(), repositorio)

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertNull(estado.perfil)
        assertEquals("No se pudo cargar tu perfil.", estado.mensaje)
        assertFalse(estado.sinConexion)
    }

    @Test
    fun `sin conexion se marca el estado sin conexion`() {
        val repositorio = FakePerfilRepositorio()
        repositorio.resultado =
            null to "No hay conexión con el servidor. Inténtalo de nuevo."
        val viewModel = PerfilViewModel(sesionAbierta(), repositorio)

        viewModel.cargar()

        assertTrue(viewModel.uiState.value.sinConexion)
    }

    @Test
    fun `reintentar tras un error carga el perfil y limpia el error`() {
        val repositorio = FakePerfilRepositorio()
        val bueno = repositorio.resultado
        repositorio.resultado = null to "No hay conexión con el servidor. Inténtalo de nuevo."
        val viewModel = PerfilViewModel(sesionAbierta(), repositorio)
        viewModel.cargar()

        repositorio.resultado = bueno
        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("Socio 1", estado.perfil!!.codigoSocio)
        assertNull(estado.mensaje)
        assertFalse(estado.sinConexion)
        assertEquals(2, repositorio.llamadas)
    }

    @Test
    fun `cerrar sesion borra el token y lo avisa una sola vez`() {
        val sesion = sesionAbierta()
        val viewModel = PerfilViewModel(sesion, FakePerfilRepositorio())

        viewModel.cerrarSesion()

        assertNull(sesion.token())
        assertTrue(viewModel.uiState.value.sesionCerrada)

        viewModel.sesionCerradaAtendida()

        assertFalse(viewModel.uiState.value.sesionCerrada)
    }
}
