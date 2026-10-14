package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Píldora de estado (pendiente, transcribiendo, transcrita…): siempre con
 * etiqueta de texto, nunca solo color (regla de accesibilidad de DESIGN.md).
 */
@Composable
fun ChipEstado(
    texto: String,
    fondo: Color,
    colorTexto: Color,
    modificador: Modifier = Modifier
) {
    Surface(
        modifier = modificador,
        shape = FormasOlivApp.extraLarge,
        color = fondo
    ) {
        Text(
            text = texto,
            style = TextoEtiqueta,
            color = colorTexto,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
