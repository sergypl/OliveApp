package com.olivapp.app.perfil.domain

/**
 * Iniciales del avatar a partir del email del socio (no hay nombre en la
 * cuenta). Con "juan.perez@..." salen "JP"; con una sola palabra, sus dos
 * primeras letras; sin letras, "?".
 */
fun inicialesDe(email: String): String {
    val local = email.substringBefore('@')
    val partes = local.split('.', '_', '-', '+').filter { parte -> parte.any { it.isLetter() } }
    val letras = if (partes.size >= 2) {
        partes[0].first { it.isLetter() }.toString() + partes[1].first { it.isLetter() }
    } else {
        local.filter { it.isLetter() }.take(2)
    }
    return letras.ifEmpty { "?" }.uppercase()
}
