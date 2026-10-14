package com.olivapp.app.parcelas.data

import com.olivapp.app.core.red.ServicioCuentas
import com.olivapp.app.parcelas.domain.DatosParcela
import com.olivapp.app.parcelas.domain.ParcelasRepositorio

/**
 * Parcelas del socio contra el backend: delega en el gateway HTTP
 * (`ServicioCuentas`), que responde siempre en el hilo principal.
 */
class ParcelasRepositorioImpl : ParcelasRepositorio {

    override fun cargarParcelas(
        token: String,
        alTerminar: (lista: List<DatosParcela>?, mensaje: String) -> Unit
    ) {
        ServicioCuentas.cargarParcelas(token, alTerminar)
    }

    override fun crearParcela(
        token: String,
        nombre: String,
        alTerminar: (exitosa: Boolean, mensaje: String) -> Unit
    ) {
        ServicioCuentas.crearParcela(token, nombre, alTerminar)
    }
}
