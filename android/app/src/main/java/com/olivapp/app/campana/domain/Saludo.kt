package com.olivapp.app.campana.domain

/**
 * Saludo del panel según la hora local (0-23): "Buenos días" de 6:00 a 13:59,
 * "Buenas tardes" de 14:00 a 20:59 y "Buenas noches" el resto, que es como se
 * saluda en España (la comida marca el paso de la mañana a la tarde).
 */
fun saludoSegunHora(hora: Int): String = when (hora) {
    in 6..13 -> "Buenos días"
    in 14..20 -> "Buenas tardes"
    else -> "Buenas noches"
}

/** Saludo de la cabecera: con el código de socio si se conoce ("Buenos días, Socio 1"). */
fun saludoDeCabecera(hora: Int, codigoSocio: String?): String {
    val saludo = saludoSegunHora(hora)
    return if (codigoSocio.isNullOrBlank()) saludo else "$saludo, $codigoSocio"
}
