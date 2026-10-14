package com.olivapp.app.campana.domain

/**
 * Campaña oleícola de una fecha ISO (AAAA-MM-DD): del 1 de noviembre al 31 de
 * octubre (misma regla que el backend en `labores.campana_de_la_fecha`).
 */
fun idCampanaDeLaFecha(fechaIso: String): String {
    val fecha = java.time.LocalDate.parse(fechaIso)
    val inicio = if (fecha.monthValue >= 11) fecha.year else fecha.year - 1
    return "$inicio-${inicio + 1}"
}
