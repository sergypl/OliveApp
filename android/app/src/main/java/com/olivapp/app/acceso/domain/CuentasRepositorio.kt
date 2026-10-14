package com.olivapp.app.acceso.domain

/**
 * Creación de cuenta e inicio de sesión del socio. Responde siempre en el hilo
 * principal con un mensaje claro para enseñar tal cual en pantalla.
 */
interface CuentasRepositorio {

    /** Crea la cuenta; `exitosa` indica si el backend la aceptó. */
    fun crearCuenta(
        email: String,
        contrasena: String,
        codigoSocio: String,
        alTerminar: (exitosa: Boolean, mensaje: String) -> Unit
    )

    /** Inicia sesión; devuelve el token (o null) y un mensaje claro. */
    fun iniciarSesion(
        email: String,
        contrasena: String,
        alTerminar: (token: String?, mensaje: String) -> Unit
    )
}
