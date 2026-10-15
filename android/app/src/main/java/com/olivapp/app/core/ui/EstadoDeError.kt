package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Pantalla de error con icono, título, motivo y botón "Reintentar". Se usa
 * cuando falla la carga de datos y no hay nada que enseñar: así un fallo de
 * conexión nunca se confunde con "no tienes datos" (el estado vacío).
 */
@Composable
fun EstadoDeError(
    titulo: String,
    mensaje: String,
    sinConexion: Boolean,
    alReintentar: () -> Unit,
    modificador: Modifier = Modifier
) {
    Column(
        modifier = modificador.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (sinConexion) R.drawable.ic_sin_conexion else R.drawable.ic_error
            ),
            contentDescription = null,
            tint = Error,
            modifier = Modifier.size(56.dp)
        )
        Text(text = titulo, style = TextoValor, color = Texto, textAlign = TextAlign.Center)
        Text(text = mensaje, style = TextoCuerpo, color = Error, textAlign = TextAlign.Center)
        BotonSecundario(
            texto = "Reintentar",
            icono = R.drawable.ic_refrescar,
            alPulsar = alReintentar
        )
    }
}
