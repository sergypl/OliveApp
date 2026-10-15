package com.olivapp.app.campana.domain

import com.olivapp.app.core.formatos.textoVariacion
import com.olivapp.app.core.formatos.variacionPorcentual
import kotlin.math.abs
import kotlin.math.floor

/**
 * Comparativa del aceite obtenido entre la campaña actual y la anterior
 * (panel). La cifra de cada campaña es el dato oficial de la cooperativa.
 *
 * - [variacion]: porcentaje entero con signo, o `null` si no se puede comparar
 *   (alguna de las dos cifras es 0: no hay base sobre la que calcular un %).
 * - [sube]: `true` si el aceite sube, `false` si baja, `null` si no cambia o no
 *   hay variación que enseñar.
 */
class ComparativaAceite(
    val campanaActual: String,
    val campanaAnterior: String,
    val aceiteActualKg: Double,
    val aceiteAnteriorKg: Double,
    val variacion: Int?
) {
    val sube: Boolean? get() = variacion?.let { if (it == 0) null else it > 0 }

    /** "+48 %", "−12 %" o "0 %"; `null` si no hay variación que enseñar. */
    val textoVariacion: String? get() = variacion?.let { textoVariacion(it.toDouble()) }

    /** Nombre corto de la campaña anterior para la referencia ("24/25"). */
    val etiquetaAnterior: String get() = etiquetaCorta(campanaAnterior)
}

/** "2024/2025" -> "24/25". Cualquier otro formato se devuelve tal cual. */
fun etiquetaCorta(nombre: String): String {
    val partes = nombre.split("/")
    if (partes.size != 2 || partes.any { p -> p.length != 4 || !p.all { it.isDigit() } }) return nombre
    return partes[0].takeLast(2) + "/" + partes[1].takeLast(2)
}

/**
 * Campaña anterior a la actual: la cerrada más reciente de las disponibles.
 * Sin campaña actual, sin cerradas o sin su cifra de aceite no hay comparativa
 * (`null`) y el panel no la enseña.
 */
fun calcularComparativa(campanas: List<DatosCampanaResumen>): ComparativaAceite? {
    val actual = campanas.firstOrNull { it.estado == "actual" } ?: return null
    val anterior = campanas
        .filter { it.estado == "cerrada" && it.aceiteKg != null }
        .maxByOrNull { it.id }
        ?: return null
    val aceiteActual = actual.aceiteKg ?: return null
    val aceiteAnterior = anterior.aceiteKg ?: return null
    return ComparativaAceite(
        campanaActual = actual.nombre,
        campanaAnterior = anterior.nombre,
        aceiteActualKg = aceiteActual,
        aceiteAnteriorKg = aceiteAnterior,
        variacion = variacionPorcentual(aceiteActual, aceiteAnterior)?.let(::redondearPorcentaje)
    )
}

/** Redondeo al entero más cercano, con los medios alejándose del cero (+2,5 -> +3; -2,5 -> -3). */
internal fun redondearPorcentaje(porcentaje: Double): Int {
    val redondeado = floor(abs(porcentaje) + 0.5).toInt()
    return if (porcentaje < 0) -redondeado else redondeado
}

/** Una barra del gráfico de aceite de las campañas disponibles. */
class BarraDeAceite(val campana: String, val aceiteKg: Double, val esActual: Boolean)

/**
 * Barras del gráfico, de la campaña más antigua a la actual. Las campañas sin
 * cifra de aceite se omiten. Con menos de dos barras no hay nada que comparar.
 */
fun barrasDeAceite(campanas: List<DatosCampanaResumen>): List<BarraDeAceite> {
    val barras = campanas
        .filter { it.aceiteKg != null }
        .sortedBy { it.id }
        .map { BarraDeAceite(it.nombre, it.aceiteKg ?: 0.0, it.estado == "actual") }
    return if (barras.size >= 2) barras else emptyList()
}
