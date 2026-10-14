package com.olivapp.app.cuaderno.domain

/** Fecha de hoy en formato español (DD/MM/AAAA). */
internal fun fechaDeHoy(): String {
    val hoy = java.time.LocalDate.now()
    return "%02d/%02d/%04d".format(hoy.dayOfMonth, hoy.monthValue, hoy.year)
}

/** Fecha en formato español (DD/MM/AAAA) a ISO (AAAA-MM-DD), o null si no vale. */
internal fun fechaIso(texto: String): String? {
    val partes = texto.trim().split("/")
    if (partes.size != 3) return null
    val dia = partes[0].toIntOrNull() ?: return null
    val mes = partes[1].toIntOrNull() ?: return null
    val anio = partes[2].toIntOrNull() ?: return null
    return try {
        java.time.LocalDate.of(anio, mes, dia).toString()
    } catch (e: Exception) {
        null
    }
}
