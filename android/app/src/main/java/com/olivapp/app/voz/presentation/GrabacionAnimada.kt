package com.olivapp.app.voz.presentation

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olivapp.app.core.ui.Borde
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoBoton
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.voz.domain.MUESTRAS_DE_ONDA
import com.olivapp.app.voz.domain.fraccionRestante
import com.olivapp.app.voz.domain.segundosRestantes

/**
 * El socio ha pedido animaciones reducidas (escala de animaciones a 0 en los
 * ajustes del sistema): se deja la onda y se muestra un indicador estático.
 */
private fun animacionesReducidas(contexto: Context): Boolean = try {
    Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
} catch (e: Exception) {
    false
}

/**
 * Control de grabación de la feature grabacion-voz-animada: botón grande
 * "Grabar"/"Parar" rodeado de un anillo de cuenta atrás de
 * [limiteSegundos], con la onda de la voz debajo y vibración al empezar y al
 * terminar. El contador en texto va siempre, también con animaciones reducidas.
 */
@Composable
fun ControlDeGrabacion(
    grabando: Boolean,
    segundos: Int,
    limiteSegundos: Int,
    nivelesOnda: List<Float>,
    alPulsar: () -> Unit
) {
    val reducidas = animacionesReducidas(LocalContext.current)

    VibrarAlEmpezarYTerminar(grabando)

    // El anillo se vacía de forma continua durante cada segundo (o a saltos de
    // un segundo si hay animaciones reducidas).
    val fraccion by animateFloatAsState(
        targetValue = when {
            !grabando -> 1f
            reducidas -> fraccionRestante(segundos.toFloat(), limiteSegundos)
            else -> fraccionRestante((segundos + 1).toFloat(), limiteSegundos)
        },
        animationSpec = if (grabando && !reducidas) tween(1000, easing = LinearEasing) else snap(),
        label = "anilloCuentaAtras"
    )
    val restantes = segundosRestantes(segundos, limiteSegundos)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(156.dp)
                .semantics {
                    if (grabando) contentDescription = "Quedan $restantes segundos de grabación"
                },
            contentAlignment = Alignment.Center
        ) {
            if (grabando) {
                AnilloDeCuentaAtras(fraccion = fraccion, modifier = Modifier.size(156.dp))
            }
            Button(
                onClick = alPulsar,
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (grabando) Error else Primario,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (grabando) "Parar" else "Grabar",
                    style = TextoBoton,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (grabando) {
            if (reducidas) {
                IndicadorDeGrabacionEstatico()
            } else {
                OndaDeVoz(niveles = nivelesOnda)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = if (grabando) {
                "Grabando… quedan $restantes de $limiteSegundos segundos"
            } else {
                "Toca el botón para empezar a grabar."
            },
            style = TextoCuerpo,
            color = if (grabando) Error else Texto,
            textAlign = TextAlign.Center
        )
    }
}

/** Vibración corta al empezar y al terminar (sin permiso VIBRATE). */
@Composable
private fun VibrarAlEmpezarYTerminar(grabando: Boolean) {
    val haptico = LocalHapticFeedback.current
    // Arranca igual que el estado actual para no vibrar al abrir la pantalla ni
    // al rotarla, solo cuando la grabación cambia de verdad.
    var anterior by remember { mutableStateOf(grabando) }
    LaunchedEffect(grabando) {
        if (grabando != anterior) {
            haptico.performHapticFeedback(HapticFeedbackType.LongPress)
            anterior = grabando
        }
    }
}

/** Anillo que se vacía de 1 a 0 a medida que se acerca el límite de la nota. */
@Composable
private fun AnilloDeCuentaAtras(fraccion: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val grosor = 8.dp.toPx()
        val lado = size.minDimension - grosor
        val esquina = Offset(grosor / 2, grosor / 2)
        drawArc(
            color = Borde,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = esquina,
            size = Size(lado, lado),
            style = Stroke(width = grosor)
        )
        drawArc(
            color = Error,
            startAngle = -90f,
            sweepAngle = 360f * fraccion,
            useCenter = false,
            topLeft = esquina,
            size = Size(lado, lado),
            style = Stroke(width = grosor, cap = StrokeCap.Round)
        )
    }
}

/** Onda de barras que sigue la amplitud real del micrófono. */
@Composable
private fun OndaDeVoz(niveles: List<Float>) {
    val altoMaximo = 64.dp
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(altoMaximo)
            .semantics { contentDescription = "Onda de tu voz" }
    ) {
        val muestras = MUESTRAS_DE_ONDA
        val hueco = 4.dp.toPx()
        val anchoBarra = ((size.width - hueco * (muestras - 1)) / muestras).coerceAtLeast(2.dp.toPx())
        val minimo = 4.dp.toPx()
        // Las muestras más recientes quedan a la derecha.
        val relleno = muestras - niveles.size
        for (i in 0 until muestras) {
            val nivel = niveles.getOrElse(i - relleno) { 0f }
            val alto = minimo + (size.height - minimo) * nivel
            val x = i * (anchoBarra + hueco)
            drawRoundRect(
                color = Error,
                topLeft = Offset(x, (size.height - alto) / 2),
                size = Size(anchoBarra, alto),
                cornerRadius = CornerRadius(anchoBarra / 2)
            )
        }
    }
}

/** Indicador sin movimiento para quien tiene las animaciones desactivadas. */
@Composable
private fun IndicadorDeGrabacionEstatico() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(16.dp)) { drawCircle(color = Error) }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Grabando", style = TextoCuerpo, color = Error, fontWeight = FontWeight.Bold)
    }
}
