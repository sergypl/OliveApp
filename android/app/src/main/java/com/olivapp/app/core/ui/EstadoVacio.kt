package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olivapp.app.R

/**
 * Estado vacío: ilustración de una hoja de olivo, texto claro y una acción.
 * Nunca deja la pantalla en blanco cuando todavía no hay nada que mostrar.
 */
@Composable
fun EstadoVacio(
    titulo: String,
    descripcion: String? = null,
    textoAccion: String? = null,
    alPulsarAccion: (() -> Unit)? = null,
    modificador: Modifier = Modifier
) {
    Column(
        modifier = modificador.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_hoja),
            contentDescription = null,
            tint = Primario,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = titulo,
            style = TextoValor,
            color = Texto,
            textAlign = TextAlign.Center
        )
        if (descripcion != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = descripcion,
                style = TextoCuerpo,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )
        }
        if (textoAccion != null && alPulsarAccion != null) {
            Spacer(modifier = Modifier.height(24.dp))
            BotonPrincipal(texto = textoAccion, alPulsar = alPulsarAccion)
        }
    }
}
