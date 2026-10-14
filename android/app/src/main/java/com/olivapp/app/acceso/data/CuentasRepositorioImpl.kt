package com.olivapp.app.acceso.data

import com.olivapp.app.acceso.domain.CuentasRepositorio
import com.olivapp.app.core.red.ServicioCuentas

/**
 * Cuentas del socio contra el backend: delega en el gateway HTTP
 * (`ServicioCuentas`), que responde siempre en el hilo principal.
 */
class CuentasRepositorioImpl : CuentasRepositorio {

    override fun crearCuenta(
        email: String,
        contrasena: String,
        codigoSocio: String,
        alTerminar: (exitosa: Boolean, mensaje: String) -> Unit
    ) {
        ServicioCuentas.crearCuenta(email, contrasena, codigoSocio, alTerminar)
    }

    override fun iniciarSesion(
        email: String,
        contrasena: String,
        alTerminar: (token: String?, mensaje: String) -> Unit
    ) {
        ServicioCuentas.iniciarSesion(email, contrasena, alTerminar)
    }
}
