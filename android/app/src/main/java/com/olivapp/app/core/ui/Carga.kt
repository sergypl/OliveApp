package com.olivapp.app.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Esqueleto de carga con brillo animado: se muestra mientras se cargan los
 * datos para que el socio vea que la pantalla está trabajando (sin texto
 * técnico y sin depender solo del color).
 */
@Composable
fun EsqueletoDeCarga(
    filas: Int = 3,
    altoFila: Dp = 28.dp,
    modificador: Modifier = Modifier
) {
    val transicion = rememberInfiniteTransition(label = "esqueleto")
    val brillo by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "brillo"
    )
    Column(modifier = modificador.fillMaxWidth()) {
        repeat(filas) { indice ->
            LineaDeEsqueleto(
                brillo = brillo,
                alto = altoFila,
                anchoFraccion = when {
                    filas == 1 -> 1f
                    indice % 3 == 0 -> 1f
                    indice % 3 == 1 -> 0.75f
                    else -> 0.55f
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LineaDeEsqueleto(brillo: Float, alto: Dp, anchoFraccion: Float) {
    val base = ContenedorBajo
    val luz = ContenedorAlto
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth(anchoFraccion)
            .height(alto)
            .padding(vertical = 2.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(base, luz, base),
                    start = Offset(x = -400f + brillo * 2400f, y = 0f),
                    end = Offset(x = 200f + brillo * 2400f, y = 0f)
                ),
                shape = FormasOlivApp.medium
            )
    )
}
