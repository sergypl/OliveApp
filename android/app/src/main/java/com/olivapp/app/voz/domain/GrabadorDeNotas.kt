package com.olivapp.app.voz.domain

import java.io.File

/**
 * Grabadora de audio de la feature de notas de voz. Puerto de dominio para
 * poder testear los ViewModels sin micrófono (la implementación real usa
 * MediaRecorder y vive en `voz/data`).
 */
interface GrabadorDeNotas {

    /** Empieza a grabar en el fichero indicado. */
    fun empezar(fichero: File)

    /** Para la grabación y devuelve los segundos grabados. */
    fun parar(): Int
}
