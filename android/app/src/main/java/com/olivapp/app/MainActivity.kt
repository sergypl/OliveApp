package com.olivapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.olivapp.app.core.navegacion.OlivApp
import com.olivapp.app.core.ui.OlivAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash de la API de splash (ADR 0010): el logo entra animado y se va
        // al primer frame, sin esperas artificiales.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            OlivAppTheme {
                OlivApp()
            }
        }
    }
}
