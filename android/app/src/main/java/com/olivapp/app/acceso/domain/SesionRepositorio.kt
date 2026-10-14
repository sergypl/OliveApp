package com.olivapp.app.acceso.domain

/** Token de la sesión del socio: si ya hay uno, la sesión sigue abierta. */
interface SesionRepositorio {

    fun guardar(token: String)

    fun token(): String?

    fun hayToken(): Boolean

    fun borrar()
}
