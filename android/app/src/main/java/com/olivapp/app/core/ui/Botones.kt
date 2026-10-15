package com.olivapp.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olivapp.app.R

/** Contenido de un botón: icono opcional (siempre acompañado del texto). */
@Composable
private fun ContenidoDeBoton(texto: String, icono: Int?, color: Color) {
    if (icono != null) {
        Icon(
            painter = painterResource(id = icono),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(text = texto, style = TextoBoton)
}

/** Botón principal: verde oliva, texto blanco y 64dp mínimo de alto. */
@Composable
fun BotonPrincipal(
    texto: String,
    habilitado: Boolean = true,
    icono: Int? = null,
    alPulsar: () -> Unit
) {
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
        ContenidoDeBoton(texto = texto, icono = icono, color = SobrePrimario)
    }
}

/** Botón secundario: contorno verde oliva, 64dp mínimo de alto. */
@Composable
fun BotonSecundario(texto: String, icono: Int? = null, alPulsar: () -> Unit) {
    OutlinedButton(
        onClick = alPulsar,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = FormasOlivApp.medium,
        border = BorderStroke(2.dp, Primario)
    ) {
        ContenidoDeBoton(texto = texto, icono = icono, color = Primario)
    }
}

/** Botón grande de acción destructiva (borrado directo, sin papelera). */
@Composable
fun BotonPeligro(texto: String, icono: Int? = null, alPulsar: () -> Unit) {
    OutlinedButton(
        onClick = alPulsar,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = FormasOlivApp.medium,
        border = BorderStroke(2.dp, Error)
    ) {
        ContenidoDeBoton(texto = texto, icono = icono, color = Error)
    }
}

/**
 * Acción grande tipo "tile" de los mockups: superficie clara, 72dp mínimo de
 * alto y texto grande centrado. Para los accesos frecuentes del panel.
 */
@Composable
fun BotonAccion(texto: String, icono: Int? = null, alPulsar: () -> Unit) {
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
        if (icono != null) {
            Icon(
                painter = painterResource(id = icono),
                contentDescription = null,
                tint = Primario,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
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

/**
 * Botón flotante extendido del micrófono: naranja de cosecha (reservado a la
 * voz) con icono y texto "Dictar nota", para el acceso rápido a las notas de
 * voz desde el panel y el cuaderno.
 */
@Composable
fun BotonMicrofonoFlotante(
    alPulsar: () -> Unit,
    texto: String = "Dictar nota",
    modificador: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = alPulsar,
        modifier = modificador.heightIn(min = 64.dp),
        shape = FormasOlivApp.medium,
        containerColor = Acento,
        contentColor = SobreAcento,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_micofono),
                contentDescription = null,
                tint = SobreAcento,
                modifier = Modifier.size(32.dp)
            )
        },
        text = {
            Text(
                text = texto,
                style = TextoBoton,
                color = SobreAcento
            )
        }
    )
}
