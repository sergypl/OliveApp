package com.olivapp.app.perfil.domain

/**
 * Perfil del socio (solo lectura): vista compuesta por la cuenta, su socio,
 * la cooperativa, sus parcelas y la campaña actual (docs/domain-model.md).
 */
data class DatosPerfil(
    val codigoSocio: String,
    val email: String,
    val cooperativa: String,
    val numeroParcelas: Int,
    val campanaActual: String
)
