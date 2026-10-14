package com.olivapp.app.acceso.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests del ViewModel de creación de cuenta (feature acceso-y-sesion). */
class RegistroViewModelTest {

    @Test
    fun `lo que teclea el socio se refleja en el estado`() {
        val viewModel = RegistroViewModel(FakeCuentasRepositorio())

        viewModel.cambiarEmail("ana@example.com")
        viewModel.cambiarContrasena("12345678")
        viewModel.cambiarCodigoSocio("Socio 1")

        val estado = viewModel.uiState.value
        assertEquals("ana@example.com", estado.email)
        assertEquals("12345678", estado.contrasena)
        assertEquals("Socio 1", estado.codigoSocio)
    }

    @Test
    fun `crear cuenta exitosa ensena el aviso y el paso a iniciar sesion`() {
        val cuentas = FakeCuentasRepositorio()
        val viewModel = RegistroViewModel(cuentas)

        viewModel.cambiarEmail("ana@example.com")
        viewModel.cambiarContrasena("12345678")
        viewModel.cambiarCodigoSocio("Socio 1")
        viewModel.crearCuenta()

        val estado = viewModel.uiState.value
        assertEquals("ana@example.com", cuentas.emailLlamada)
        assertEquals("12345678", cuentas.contrasenaLlamada)
        assertEquals("Socio 1", cuentas.codigoSocioLlamada)
        assertTrue(estado.registrada)
        assertEquals("Cuenta creada. Ya puedes iniciar sesión.", estado.mensaje)
        assertFalse(estado.trabajando)
    }

    @Test
    fun `crear cuenta fallida ensena el mensaje del backend sin marcar exito`() {
        val cuentas = FakeCuentasRepositorio()
        cuentas.resultadoCrear = false to "Ese email ya está registrado. Inicia sesión."
        val viewModel = RegistroViewModel(cuentas)

        viewModel.crearCuenta()

        val estado = viewModel.uiState.value
        assertFalse(estado.registrada)
        assertEquals("Ese email ya está registrado. Inicia sesión.", estado.mensaje)
        assertFalse(estado.trabajando)
    }

    @Test
    fun `mientras el backend no responde la cuenta se esta creando`() {
        val cuentas = FakeCuentasRepositorio(responderAlMomento = false)
        val viewModel = RegistroViewModel(cuentas)

        viewModel.crearCuenta()

        assertTrue(viewModel.uiState.value.trabajando)

        cuentas.pendienteCrear!!.invoke(true, "Cuenta creada. Ya puedes iniciar sesión.")

        val estado = viewModel.uiState.value
        assertFalse(estado.trabajando)
        assertTrue(estado.registrada)
    }
}
