package com.olivapp.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olivapp.app.R

/** Botón principal: verde oliva, texto blanco y 64dp mínimo de alto. */
@Composable
fun BotonPrincipal(texto: String, habilitado: Boolean = true, alPulsar: () -> Unit) {
    Button(
        onClick = alPulsar,
        enabled = habilitado,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = FormasOlivApp.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Primario,
            contentColor = SobrePrimario
        )
    ) {
        Text(text = texto, style = TextoBoton)
    }
}

/** Botón secundario: contorno verde oliva, 64dp mínimo de alto. */
@Composable
fun BotonSecundario(texto: String, alPulsar: () -> Unit) {
    OutlinedButton(
        onClick = alPulsar,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = FormasOlivApp.medium,
        border = BorderStroke(2.dp, Primario)
    ) {
        Text(text = texto, style = TextoBoton, color = Primario)
    }
}

/** Botón grande de acción destructiva (borrado directo, sin papelera). */
@Composable
fun BotonPeligro(texto: String, alPulsar: () -> Unit) {
    OutlinedButton(
        onClick = alPulsar,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = FormasOlivApp.medium,
        border = BorderStroke(2.dp, Error)
    ) {
        Text(text = texto, style = TextoBoton, color = Error)
    }
}

/**
 * Acción grande tipo "tile" de los mockups: superficie clara, 72dp mínimo de
 * alto y texto grande centrado. Para los accesos frecuentes del panel.
 */
@Composable
fun BotonAccion(texto: String, alPulsar: () -> Unit) {
    Button(
        onClick = alPulsar,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = FormasOlivApp.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = Contenedor,
            contentColor = Texto
        )
    ) {
        Text(
            text = texto,
            style = TextoBoton,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

/**
 * Botón grande de micrófono: el protagonista del panel. Naranja de cosecha,
 * circular, 96dp, con el icono en marrón oscuro (contraste AA 4,5:1 — el
 * blanco sobre este naranja no lo pasa).
 */
@Composable
fun BotonMicrofono(descripcion: String, alPulsar: () -> Unit) {
    Button(
        onClick = alPulsar,
        modifier = Modifier.size(96.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Acento,
            contentColor = SobreAcento
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_micofono),
            contentDescription = descripcion,
            modifier = Modifier.size(48.dp),
            tint = SobreAcento
        )
    }
}
