package com.olivapp.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla completa (borde a borde) de fundamentos-visuales-modernos.
 *
 * La ventana se dibuja hasta el borde de la pantalla (la barra de estado es
 * transparente con iconos oscuros) y el contenido se aparta de la barra de
 * estado, del recorte de cámara, de la barra de navegación del sistema y del
 * teclado. Además se reserva el hueco de la barra de navegación inferior
 * (solo en las pantallas que la llevan) para que no tape nada.
 */

/** Altura del hueco reservado para la barra de navegación inferior. */
val AlturaBarraInferior = 80.dp

/**
 * Si la pantalla actual lleva la barra de navegación inferior. El router lo
 * fija por pantalla: las de segundo nivel y las de acceso no la llevan y no
 * reservan su hueco (feature `navegacion-barra-inferior`).
 */
val LocalBarraInferiorVisible = compositionLocalOf { true }

/** Margen de contenido de todas las pantallas (DESIGN.md: ritmo 4/8dp). */
private val MargenContenido = 24.dp

/**
 * Modificador de la raíz de cada pantalla: fondo a sangre, márgenes de
 * pantalla completa y margen de contenido. Con el teclado abierto el hueco de
 * la barra inferior se retira para no desperdiciar espacio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.pantallaCompleta(reservaBarraInferior: Boolean = true): Modifier {
    val barraVisible = LocalBarraInferiorVisible.current
    val huecoInferior =
        if (reservaBarraInferior && barraVisible && !WindowInsets.isImeVisible) {
            AlturaBarraInferior
        } else {
            0.dp
        }
    return this
        .background(Fondo)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom
            )
        )
        .padding(
            start = MargenContenido,
            end = MargenContenido,
            top = MargenContenido,
            bottom = MargenContenido + huecoInferior
        )
}
