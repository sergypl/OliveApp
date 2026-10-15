package com.olivapp.app

import android.app.Application
import com.olivapp.app.voz.data.AlmacenNotasVoz
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada de la app y raíz del grafo de inyección de dependencias
 * (ADR 0008: DI con Hilt).
 */
@HiltAndroidApp
class AplicacionOlivApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Al arrancar no hay ninguna transcripción en curso: es el momento
        // seguro de reanudar las notas que se quedaron 'transcribiendo' cuando
        // el proceso murió, para que vuelvan a la cola y se reintenten.
        AlmacenNotasVoz(this).reanudarTranscripcionesPendientes()
    }
}
