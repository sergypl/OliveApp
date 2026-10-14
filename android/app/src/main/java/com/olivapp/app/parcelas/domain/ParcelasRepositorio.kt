package com.olivapp.app.parcelas.domain

/**
 * Parcelas del socio. En el MVP no se eliminan ni archivan
 * (docs/domain-model.md): solo se consultan y se crean.
 */
interface ParcelasRepositorio {

    fun cargarParcelas(
        token: String,
        alTerminar: (lista: List<DatosParcela>?, mensaje: String) -> Unit
    )

    fun crearParcela(
        token: String,
        nombre: String,
        alTerminar: (exitosa: Boolean, mensaje: String) -> Unit
    )
}
