package com.olivapp.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada de la app y raíz del grafo de inyección de dependencias
 * (ADR 0008: DI con Hilt).
 */
@HiltAndroidApp
class AplicacionOlivApp : Application()
