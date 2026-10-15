package com.olivapp.app.campana.domain

/**
 * Campaña oleícola de una fecha ISO (AAAA-MM-DD): del 1 de noviembre al 31 de
 * octubre (misma regla que el backend en `labores.campana_de_la_fecha`).
 *
 * La fecha llega del borrador IA y solo está validada de forma: una que no
 * existe ("2026-02-30") se devuelve como nulo en vez de reventar con ella.
 */
fun idCampanaDeLaFecha(fechaIso: String): String? {
    val fecha = try {
        java.time.LocalDate.parse(fechaIso)
    } catch (e: Exception) {
        return null
    }
    val inicio = if (fecha.monthValue >= 11) fecha.year else fecha.year - 1
    return "$inicio-${inicio + 1}"
}
