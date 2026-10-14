package com.olivapp.app.acceso.presentation

import com.olivapp.app.acceso.domain.CuentasRepositorio
import com.olivapp.app.acceso.domain.SesionRepositorio

/**
 * Fakes de los repositorios de acceso para testear los ViewModels sin red ni
 * Hilt: responden al momento o retienen la llamada para simular espera.
 */
class FakeCuentasRepositorio(
    private val responderAlMomento: Boolean = true
) : CuentasRepositorio {

    var resultadoCrear: Pair<Boolean, String> = true to "Cuenta creada. Ya puedes iniciar sesión."
    var resultadoIniciar: Pair<String?, String> = "token-abc" to ""

    var emailLlamada: String? = null
    var contrasenaLlamada: String? = null
    var codigoSocioLlamada: String? = null

    var pendienteCrear: ((Boolean, String) -> Unit)? = null
    var pendienteIniciar: ((String?, String) -> Unit)? = null

    override fun crearCuenta(
        email: String,
        contrasena: String,
        codigoSocio: String,
        alTerminar: (exitosa: Boolean, mensaje: String) -> Unit
    ) {
        emailLlamada = email
        contrasenaLlamada = contrasena
        codigoSocioLlamada = codigoSocio
        if (responderAlMomento) {
            alTerminar(resultadoCrear.first, resultadoCrear.second)
        } else {
            pendienteCrear = alTerminar
        }
    }

    override fun iniciarSesion(
        email: String,
        contrasena: String,
        alTerminar: (token: String?, mensaje: String) -> Unit
    ) {
        emailLlamada = email
        contrasenaLlamada = contrasena
        if (responderAlMomento) {
            alTerminar(resultadoIniciar.first, resultadoIniciar.second)
        } else {
            pendienteIniciar = alTerminar
        }
    }
}

class FakeSesionRepositorio : SesionRepositorio {

    private var guardado: String? = null

    override fun guardar(token: String) {
        guardado = token
    }

    override fun token(): String? = guardado

    override fun hayToken(): Boolean = guardado != null

    override fun borrar() {
        guardado = null
    }
}
