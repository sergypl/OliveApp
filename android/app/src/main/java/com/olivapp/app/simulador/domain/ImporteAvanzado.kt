package com.olivapp.app.simulador.domain

import kotlin.math.abs

/** Gastos de la cooperativa por defecto, en €/kg de aceite (editables). */
const val GASTOS_POR_DEFECTO = 0.20

/** Reparto del aceite por calidad, en porcentaje de cada categoría. */
data class RepartoCalidad(
    val aove: Double = 100.0,
    val virgen: Double = 0.0,
    val lampante: Double = 0.0
) {
    val suma: Double get() = aove + virgen + lampante

    /** Suma 100 % (con la tolerancia de un redondeo de céntimo de punto). */
    val sumaCien: Boolean get() = abs(suma - 100.0) < 0.005

    fun porcentaje(categoria: CategoriaAceite): Double = when (categoria) {
        CategoriaAceite.AOVE -> aove
        CategoriaAceite.VIRGEN -> virgen
        CategoriaAceite.LAMPANTE -> lampante
    }

    companion object {
        /** 100 % de una sola categoría (reparto por defecto del simulador). */
        fun completo(categoria: CategoriaAceite) = RepartoCalidad(
            aove = if (categoria == CategoriaAceite.AOVE) 100.0 else 0.0,
            virgen = if (categoria == CategoriaAceite.VIRGEN) 100.0 else 0.0,
            lampante = if (categoria == CategoriaAceite.LAMPANTE) 100.0 else 0.0
        )
    }
}

/**
 * Precio ponderado (€/kg) con los precios de una misma semana y mercado.
 * Devuelve null si el reparto no suma 100 % o si falta el precio de una
 * categoría con porcentaje: nunca se inventa un precio.
 */
fun precioPonderado(
    precios: Map<CategoriaAceite, Double>,
    reparto: RepartoCalidad
): Double? {
    if (!reparto.sumaCien) return null
    var total = 0.0
    for (categoria in CategoriaAceite.entries) {
        val porcentaje = reparto.porcentaje(categoria)
        if (porcentaje <= 0.0) continue
        val precio = precios[categoria] ?: return null
        total += precio * porcentaje / 100.0
    }
    return total
}

/** Importes estimados del simulador avanzado (siempre una estimación). */
class ResultadoAvanzado(
    val precio: Double,
    val gastosPorKilo: Double,
    val importeBruto: Double,
    val gastos: Double,
    val importeNeto: Double,
    /** Importe neto repartido entre los kilos de aceituna entregados. */
    val netoPorKiloDeAceituna: Double
) {
    /** Los gastos superan al precio: el importe neto sale negativo. */
    val gastosSuperanElPrecio: Boolean get() = gastosPorKilo > precio
}

/**
 * Importe estimado: bruto = aceite neto × precio; gastos = aceite neto ×
 * gastos (€/kg de aceite); neto = bruto − gastos; y su equivalente en €/kg de
 * aceituna. Devuelve null si falta un dato para calcular (precio, kilos o
 * aceite neto no positivos): el precio 0 no es un precio, es "sin precio".
 * Los gastos mayores que el precio dan un neto negativo, que se avisa aparte.
 */
fun calcularImporteAvanzado(
    aceiteNeto: Double,
    kilosDeAceituna: Double,
    precio: Double,
    gastosPorKilo: Double
): ResultadoAvanzado? {
    if (aceiteNeto <= 0.0 || kilosDeAceituna <= 0.0 || precio <= 0.0 || gastosPorKilo < 0.0) {
        return null
    }
    val bruto = aceiteNeto * precio
    val gastos = aceiteNeto * gastosPorKilo
    val neto = bruto - gastos
    return ResultadoAvanzado(precio, gastosPorKilo, bruto, gastos, neto, neto / kilosDeAceituna)
}
