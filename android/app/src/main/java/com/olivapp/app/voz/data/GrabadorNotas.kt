package com.olivapp.app.voz.data

import android.media.MediaRecorder
import java.io.File

/** Grabadora de una nota de voz: MPEG-4 con audio AAC. */
class GrabadorNotas {
    private var grabador: MediaRecorder? = null
    private var empezadoEn = 0L

    @Suppress("DEPRECATION")
    fun empezar(fichero: File) {
        val r = MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(16000)
        r.setOutputFile(fichero.absolutePath)
        r.prepare()
        r.start()
        grabador = r
        empezadoEn = android.os.SystemClock.elapsedRealtime()
    }

    /** Para la grabación y devuelve los segundos grabados. */
    fun parar(): Int {
        val r = grabador ?: return 0
        grabador = null
        val segundos = ((android.os.SystemClock.elapsedRealtime() - empezadoEn) / 1000).toInt()
        try {
            r.stop()
        } catch (e: Exception) {
            // Grabación demasiado corta: stop() falla y el fichero no sirve.
        }
        r.release()
        return segundos
    }
}
