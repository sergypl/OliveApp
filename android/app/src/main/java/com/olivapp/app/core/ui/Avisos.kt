package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Aviso con acción "Deshacer" (snackbar) y su host, para deshacer operaciones
 * sin confirmaciones interminables. El texto va en frases completas y la
 * acción siempre con texto, nunca solo icono.
 */

/** Host del aviso, colocado por la pantalla que lo use (abajo, sobre todo). */
@Composable
fun HostDeAvisos(estado: SnackbarHostState, modificador: Modifier = Modifier) {
    SnackbarHost(hostState = estado, modifier = modificador) { datos ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = ContenedorMasAlto,
            contentColor = Texto,
            action = {
                datos.visuals.actionLabel?.let { etiqueta ->
                    TextButton(onClick = { datos.performAction() }) {
                        Text(text = etiqueta, style = TextoEtiqueta, color = Primario)
                    }
                }
            },
            dismissAction = {
                if (datos.visuals.withDismissAction) {
                    TextButton(onClick = { datos.dismiss() }) {
                        Text(text = "Cerrar", style = TextoEtiqueta, color = Primario)
                    }
                }
            }
        ) {
            Text(text = datos.visuals.message, style = TextoCuerpo)
        }
    }
}

/**
 * Muestra un aviso con acción "Deshacer" y devuelve `true` si el socio pulsó
 * deshacer antes de que el mensaje se cerrara. Se llama desde una corrutina.
 */
suspend fun SnackbarHostState.mostrarConDeshacer(
    texto: String,
    textoAccion: String = "Deshacer"
): Boolean {
    val resultado = showSnackbar(
        message = texto,
        actionLabel = textoAccion,
        withDismissAction = true,
        duration = androidx.compose.material3.SnackbarDuration.Short
    )
    return resultado == SnackbarResult.ActionPerformed
}
