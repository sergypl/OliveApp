package com.olivapp.app.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.olivapp.app.core.formatos.formatearCantidad

/**
 * Cifra destacada que se anima al cambiar (por ejemplo, 180 → 195 kg): el
 * número recorre los valores intermedios en vez de saltar. Va siempre con su
 * unidad dentro del texto.
 */
@Composable
fun CifraAnimada(
    valor: Double,
    sufijo: String = "",
    modificador: Modifier = Modifier
) {
    val animado by animateFloatAsState(
        targetValue = valor.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "cifra"
    )
    Text(
        text = formatearCantidad(animado.toDouble()) + sufijo,
        style = TextoCifra,
        color = Primario,
        textAlign = TextAlign.Center,
        modifier = modificador
    )
}
