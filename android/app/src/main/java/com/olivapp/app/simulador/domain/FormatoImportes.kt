package com.olivapp.app.simulador.domain

import com.olivapp.app.core.formatos.formatearNumero
import kotlin.math.abs
import kotlin.math.round

/** Importe en euros con formato español: 1.234,50 €. */
fun formatearEuros(valor: Double): String = formatearNumero(valor, 2) + " €"

/**
 * Precio por kilo con formato español: dos decimales si bastan (4,45 €/kg) y
 * tres si el precio los necesita (el ponderado, 3,855 €/kg).
 */
fun formatearEurosPorKilo(valor: Double, decimalesMinimos: Int = 2): String {
    val bastanDos = abs(valor * 100.0 - round(valor * 100.0)) < 1e-6
    val decimales = if (bastanDos) decimalesMinimos else 3
    return formatearNumero(valor, decimales) + " €/kg"
}
