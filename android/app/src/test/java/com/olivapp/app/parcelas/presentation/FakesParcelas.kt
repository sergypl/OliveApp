package com.olivapp.app.parcelas.presentation

import com.olivapp.app.parcelas.domain.DatosParcela
import com.olivapp.app.parcelas.domain.ParcelasRepositorio

/**
 * Fake del repositorio de parcelas para testear el ViewModel sin red ni Hilt:
 * responde al momento o retiene la llamada para simular espera.
 */
class FakeParcelasRepositorio(
    private val responderAlMomento: Boolean = true
) : ParcelasRepositorio {

    var resultadoCargar: Pair<List<DatosParcela>?, String> = listOf(
        DatosParcela(id = "p-1", nombre = "Cañada Honda")
    ) to ""

    var resultadoCrear: Pair<Boolean, String> = true to "Parcela creada."

    var tokenLlamada: String? = null
    var nombreLlamada: String? = null

    var pendienteCrear: ((Boolean, String) -> Unit)? = null

    override fun cargarParcelas(
        token: String,
        alTerminar: (lista: List<DatosParcela>?, mensaje: String) -> Unit
    ) {
        tokenLlamada = token
        alTerminar(resultadoCargar.first, resultadoCargar.second)
    }

    override fun crearParcela(
        token: String,
        nombre: String,
        alTerminar: (exitosa: Boolean, mensaje: String) -> Unit
    ) {
        tokenLlamada = token
        nombreLlamada = nombre
        if (responderAlMomento) {
            alTerminar(resultadoCrear.first, resultadoCrear.second)
        } else {
            pendienteCrear = alTerminar
        }
    }
}
