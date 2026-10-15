package com.olivapp.app.acceso.data

import java.util.Base64
import org.json.JSONObject

/**
 * Identidad de la cuenta de un token de sesión (el JWT del backend): la
 * reclamación "sub" es el id de la cuenta, el mismo en cada inicio de sesión
 * del socio y distinto entre cuentas. Es lo que amarra la cola de notas de voz
 * del teléfono a su dueño.
 *
 * Si el token no se puede leer como ese JWT, se devuelve nulo (la app sigue
 * funcionando, pero sin identidad de cuenta).
 */
fun idDeCuentaDe(token: String?): String? {
    val partes = token?.split(".") ?: return null
    if (partes.size != 3) return null
    return try {
        val contenido = String(Base64.getUrlDecoder().decode(partes[1]), Charsets.UTF_8)
        JSONObject(contenido).optString("sub").ifBlank { null }
    } catch (e: Exception) {
        null
    }
}
