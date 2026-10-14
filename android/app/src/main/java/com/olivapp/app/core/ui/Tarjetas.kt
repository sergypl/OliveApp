package com.olivapp.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tarjeta de contenido: superficie blanca con borde fino de 1px, radio 16dp y
 * padding de 24dp (capa tonal de los mockups de Stitch).
 */
@Composable
fun Tarjeta(
    modificador: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modificador.fillMaxWidth(),
        shape = FormasOlivApp.large,
        color = Superficie,
        border = BorderStroke(1.dp, Borde)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = contenido
        )
    }
}

/**
 * Cabecera de sección: banda verde oliva con texto blanco (tipo "Patio
 * Abierto" de los mockups), con un hueco opcional a la derecha.
 */
@Composable
fun CabeceraSeccion(
    titulo: String,
    derecha: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FormasOlivApp.large,
        color = Primario
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = titulo,
                style = TextoTitulo2,
                color = SobrePrimario,
                modifier = Modifier.weight(1f)
            )
            derecha?.invoke()
        }
    }
}

/** Panel destacado: superficie con tinte suave para bloques de ayuda o avisos. */
@Composable
fun PanelDestacado(
    colorFondo: Color = PanelDestacado,
    modificador: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modificador.fillMaxWidth(),
        shape = FormasOlivApp.large,
        color = colorFondo
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = contenido
        )
    }
}
