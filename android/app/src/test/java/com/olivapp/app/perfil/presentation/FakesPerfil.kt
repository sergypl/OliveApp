package com.olivapp.app.perfil.presentation

import com.olivapp.app.perfil.domain.DatosPerfil
import com.olivapp.app.perfil.domain.PerfilRepositorio

/** Fake del repositorio del perfil para testear el ViewModel sin red ni Hilt. */
class FakePerfilRepositorio : PerfilRepositorio {

    var resultado: Pair<DatosPerfil?, String> = DatosPerfil(
        codigoSocio = "Socio 1",
        email = "juan.perez@ejemplo.com",
        cooperativa = "Cooperativa Olivarera de Demostración",
        numeroParcelas = 4,
        campanaActual = "2025/2026"
    ) to ""

    var tokenLlamada: String? = null
    var llamadas = 0

    override fun cargarPerfil(
        token: String,
        alTerminar: (perfil: DatosPerfil?, mensaje: String) -> Unit
    ) {
        tokenLlamada = token
        llamadas += 1
        alTerminar(resultado.first, resultado.second)
    }
}
