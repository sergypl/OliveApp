package com.olivapp.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olivapp.app.R

/**
 * Barra superior de TODA la app (CONTEXT.md: "Barra superior"): mismo alto,
 * mismo margen y mismo título (TextoTitulo1) en todas las pantallas, con o sin
 * flecha de volver. No se encoge ni se esconde al hacer scroll: el título
 * siempre se ve y siempre está a la misma altura.
 *
 * El título va alineado con el margen de contenido de las pantallas y el hueco
 * por arriba y por abajo del título es el mismo (era la queja de "mucho
 * espacio por arriba y poco por abajo" de las barras grandes de Material).
 */
private val AltoBarraSuperior = 72.dp

@Composable
fun BarraSuperior(titulo: String, onVolver: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .background(Fondo)
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
            .height(AltoBarraSuperior),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onVolver != null) {
            // Icono a la altura del margen de contenido; diana táctil de 48dp.
            IconButton(onClick = onVolver, modifier = Modifier.padding(start = 12.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_volver),
                    contentDescription = "Volver",
                    tint = Primario
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width(MargenContenido))
        }
        Text(
            text = titulo,
            style = TextoTitulo1,
            color = Texto,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Spacer(modifier = Modifier.width(MargenContenido))
    }
}
