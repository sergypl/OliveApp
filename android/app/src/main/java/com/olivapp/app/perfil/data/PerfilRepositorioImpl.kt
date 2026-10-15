package com.olivapp.app.perfil.data

import com.olivapp.app.core.red.ServicioCuentas
import com.olivapp.app.perfil.domain.DatosPerfil
import com.olivapp.app.perfil.domain.PerfilRepositorio

/** Perfil del socio contra el backend: delega en el gateway HTTP (`ServicioCuentas`). */
class PerfilRepositorioImpl : PerfilRepositorio {

    override fun cargarPerfil(
        token: String,
        alTerminar: (perfil: DatosPerfil?, mensaje: String) -> Unit
    ) {
        ServicioCuentas.cargarMiPerfil(token, alTerminar)
    }
}
