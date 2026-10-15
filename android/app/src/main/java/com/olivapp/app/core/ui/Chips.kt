package com.olivapp.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.olivapp.app.R

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

/**
 * Chip de filtro grande para las listas (categoría, parcela…): píldora con
 * texto, zona táctil de 48dp mínimo y selección marcada con el icono de
 * verificación además del color (nunca solo el color).
 */
@Composable
fun ChipFiltro(
    texto: String,
    elegido: Boolean,
    alPulsar: () -> Unit,
    icono: Int? = null,
    modificador: Modifier = Modifier
) {
    Surface(
        modifier = modificador
            .heightIn(min = 48.dp)
            .clickable(onClick = alPulsar),
        shape = FormasOlivApp.extraLarge,
        color = if (elegido) Primario else Contenedor,
        border = BorderStroke(2.dp, if (elegido) Primario else Borde)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (elegido) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_confirmada),
                    contentDescription = "Seleccionado",
                    tint = SobrePrimario,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (icono != null) {
                Icon(
                    painter = painterResource(id = icono),
                    contentDescription = null,
                    tint = if (elegido) SobrePrimario else Texto,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = texto,
                style = TextoEtiqueta,
                color = if (elegido) SobrePrimario else Texto
            )
        }
    }
}
