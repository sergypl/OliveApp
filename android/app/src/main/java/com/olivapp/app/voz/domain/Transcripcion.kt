package com.olivapp.app.voz.domain

import com.olivapp.app.voz.data.AlmacenNotasVoz

/**
 * Aplica el resultado de una transcripción a la nota de la cola y devuelve el
 * aviso para el socio (features cola-offline-de-notas,
 * reintento-de-transcripcion-fallida y borrador-ia-desde-transcripcion).
 */
fun aplicarTranscripcion(
    almacen: AlmacenNotasVoz,
    id: String,
    texto: String?,
    borrador: String,
    error: String,
    sinConexion: Boolean,
    audioInvalido: Boolean
): String {
    return if (texto != null) {
        // La nota queda transcrita con su borrador IA (o el marcador de
        // rechazo cuando la IA no reconoce la categoría de la labor).
        almacen.marcar(id, "transcrita", texto, borrador)
        "Nota transcrita. Ya puedes leer el texto."
    } else if (sinConexion) {
        // Sin conexión la nota no se pierde: vuelve a la cola local
        // en estado 'pendiente' (feature cola-offline-de-notas).
        almacen.marcar(id, "pendiente", "")
        "Sin conexión con el servidor. Tu nota está guardada en el teléfono, pendiente de enviar."
    } else if (audioInvalido) {
        // El audio no se puede transcribir (ilegible o demasiado corto): se
        // pide repetir el dictado (feature reintento-de-transcripcion-fallida).
        almacen.marcar(id, "audio_invalido", "")
        error
    } else {
        // Fallo del servicio de transcripción: la nota queda con error visible
        // y se reintenta a mano (feature reintento-de-transcripcion-fallida).
        almacen.marcar(id, "error", "")
        error
    }
}
