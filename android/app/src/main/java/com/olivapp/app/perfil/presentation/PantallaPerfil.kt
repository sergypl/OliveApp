package com.olivapp.app.perfil.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoTitulo1
import com.olivapp.app.core.ui.pantallaCompleta

/** Pestaña Perfil: de momento solo el título (el contenido llega con `perfil-del-socio`). */
@Composable
fun PantallaPerfil() {
    Column(modifier = Modifier.pantallaCompleta().verticalScroll(rememberScrollState())) {
        Text(text = "Tu perfil", style = TextoTitulo1, color = Texto)
    }
}
