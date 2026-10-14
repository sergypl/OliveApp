package com.olivapp.app.voz.domain

import java.io.File

/**
 * La feature de notas de voz: la cola local del teléfono más la transcripción
 * de cada nota en el backend (el servidor no conserva el audio). Responde
 * siempre en el hilo principal (vía el gateway HTTP).
 */
interface NotasRepositorio : ColaDeNotas {

    /** Transcribe el audio de una nota y devuelve su ficha borrador IA. */
    fun transcribirNota(
        token: String,
        fichero: File,
        alTerminar: (
            texto: String?,
            borrador: String,
            error: String,
            sinConexion: Boolean,
            audioInvalido: Boolean
        ) -> Unit
    )

    /** Responde por voz a la pregunta del asistente y completa la ficha. */
    fun completarBorrador(
        token: String,
        fichero: File,
        borrador: String,
        alTerminar: (
            texto: String?,
            borrador: String,
            error: String,
            sinConexion: Boolean,
            audioInvalido: Boolean
        ) -> Unit
    )
}
