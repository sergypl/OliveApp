package com.olivapp.app.acceso.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests del ViewModel de inicio de sesión (feature acceso-y-sesion). */
class InicioSesionViewModelTest {

    @Test
    fun `lo que teclea el socio se refleja en el estado`() {
        val viewModel = InicioSesionViewModel(FakeCuentasRepositorio(), FakeSesionRepositorio())

        viewModel.cambiarEmail("ana@example.com")
        viewModel.cambiarContrasena("12345678")

        val estado = viewModel.uiState.value
        assertEquals("ana@example.com", estado.email)
        assertEquals("12345678", estado.contrasena)
    }

    @Test
    fun `entrar guarda el token y abre la sesion`() {
        val cuentas = FakeCuentasRepositorio()
        val sesion = FakeSesionRepositorio()
        val viewModel = InicioSesionViewModel(cuentas, sesion)

        viewModel.cambiarEmail("ana@example.com")
        viewModel.cambiarContrasena("12345678")
        viewModel.iniciarSesion()

        val estado = viewModel.uiState.value
        assertEquals("ana@example.com", cuentas.emailLlamada)
        assertEquals("12345678", cuentas.contrasenaLlamada)
        assertEquals("token-abc", sesion.token())
        assertTrue(estado.sesionAbierta)
        assertFalse(estado.trabajando)
        assertNull(estado.mensaje)
    }

    @Test
    fun `entrar fallido ensena el mensaje del backend y no guarda nada`() {
        val cuentas = FakeCuentasRepositorio()
        cuentas.resultadoIniciar = null to "Email o contraseña incorrectos."
        val sesion = FakeSesionRepositorio()
        val viewModel = InicioSesionViewModel(cuentas, sesion)

        viewModel.iniciarSesion()

        val estado = viewModel.uiState.value
        assertEquals("Email o contraseña incorrectos.", estado.mensaje)
        assertFalse(estado.sesionAbierta)
        assertNull(sesion.token())
        assertFalse(estado.trabajando)
    }

    @Test
    fun `mientras el backend no responde se esta entrando`() {
        val cuentas = FakeCuentasRepositorio(responderAlMomento = false)
        val viewModel = InicioSesionViewModel(cuentas, FakeSesionRepositorio())

        viewModel.iniciarSesion()

        assertTrue(viewModel.uiState.value.trabajando)

        cuentas.pendienteIniciar!!.invoke("token-abc", "")

        val estado = viewModel.uiState.value
        assertFalse(estado.trabajando)
        assertTrue(estado.sesionAbierta)
    }
}
