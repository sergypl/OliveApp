package com.olivapp.app.perfil.domain

/** Perfil del socio autenticado, de solo lectura. */
interface PerfilRepositorio {

    fun cargarPerfil(
        token: String,
        alTerminar: (perfil: DatosPerfil?, mensaje: String) -> Unit
    )
}
