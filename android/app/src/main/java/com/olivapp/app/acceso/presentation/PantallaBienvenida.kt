package com.olivapp.app.acceso.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.Fondo
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoTitulo1
import com.olivapp.app.core.ui.pantallaCompleta

/**
 * Pantalla de bienvenida: solo dos accesos (iniciar sesión y crear cuenta).
 * No tiene estado que recordar, así que va sin ViewModel.
 */
@Composable
fun PantallaBienvenida(onIniciarSesion: () -> Unit, onCrearCuenta: () -> Unit) {
    Column(
        modifier = Modifier.pantallaCompleta(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OlivApp",
            style = TextoTitulo1,
            color = Texto
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tus campañas y tu cuaderno de campo,\na la mano.",
            style = TextoCuerpo,
            color = Texto,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        BotonPrincipal(texto = "Iniciar sesión", alPulsar = onIniciarSesion)
        Spacer(modifier = Modifier.height(16.dp))
        BotonSecundario(texto = "Crear cuenta", alPulsar = onCrearCuenta)
    }
}
