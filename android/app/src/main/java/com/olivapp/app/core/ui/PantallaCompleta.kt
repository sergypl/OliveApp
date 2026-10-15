package com.olivapp.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Esqueleto de pantalla completa (borde a borde) de toda la app.
 *
 * La ventana se dibuja hasta el borde de la pantalla (la barra de estado es
 * transparente con iconos oscuros). Cada pantalla se compone con la barra
 * superior de toda la app y su contenido con los mismos márgenes: el título
 * siempre a la misma altura, contenido alineado con él y hueco reservado
 * abajo para que la barra de navegación inferior no tape nada.
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
val MargenContenido = 24.dp

/**
 * Margen horizontal de contenido. Cada bloque lo aplica cuando la pantalla se
 * dibuja a todo el ancho, que es lo que necesitan las filas que se deslizan a
 * los lados: van de borde a borde y los elementos pasan por debajo de los
 * márgenes sin cortarse, con el primero y el último alineados al margen.
 */
fun Modifier.margenDeContenido(): Modifier = this.padding(horizontal = MargenContenido)

/** Hueco bajo el contenido: barra de navegación inferior si la pantalla la lleva. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun huecoDeBarraInferior(): Dp =
    if (LocalBarraInferiorVisible.current && !WindowInsets.isImeVisible) {
        AlturaBarraInferior
    } else {
        0.dp
    }

/**
 * Pantalla con la barra superior de toda la app y contenido sencillo que se
 * desplaza con la pantalla (formularios, textos, tarjetas). El hueco `fijo`
 * (opcional) queda FUERA del desplazamiento, entre la barra y el contenido:
 * sirve para lo que no se debe ir de pantalla, como la tarjeta de resultado
 * del simulador.
 */
@Composable
fun PantallaConBarraSuperior(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    fijo: (@Composable () -> Unit)? = null,
    contenido: @Composable () -> Unit
) {
    val huecoInferior = MargenContenido + huecoDeBarraInferior()
    Column(
        modifier = Modifier
            .background(Fondo)
            .fillMaxSize()
    ) {
        BarraSuperior(titulo = titulo, onVolver = onVolver)
        Column(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .padding(start = MargenContenido, end = MargenContenido)
                .fillMaxSize()
        ) {
            if (fijo != null) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    fijo()
                }
            }
            Column(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = huecoInferior)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                contenido()
            }
        }
    }
}

/**
 * Variante para contenido que maneja su propio desplazamiento (la línea de
 * tiempo de "Mi cuaderno", el panel con refresco, formularios propios o una
 * composición centrada como la de bienvenida). El hueco de la barra de
 * navegación inferior se reserva aquí, y el aviso con "Deshacer" y el botón
 * flotante se dibujan ENCIMA de esa barra (no debajo, como pasaba con el
 * `Scaffold` de Material, que los dejaba tapados).
 *
 * Con `margenLateral = false` el contenido va a todo el ancho de la pantalla y
 * cada bloque aplica su propio margen con [margenDeContenido]: es lo que
 * necesitan las filas de chips que se deslizan a los lados, para que pasen por
 * debajo de los márgenes sin cortarse.
 */
@Composable
fun PantallaConBarraSuperiorEnLista(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    aviso: SnackbarHostState? = null,
    accionFlotante: (@Composable () -> Unit)? = null,
    margenLateral: Boolean = true,
    contenido: @Composable () -> Unit
) {
    val huecoDeBarra = huecoDeBarraInferior()
    val huecoInferior = MargenContenido + huecoDeBarra
    Box(
        modifier = Modifier
            .background(Fondo)
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BarraSuperior(titulo = titulo, onVolver = onVolver)
            Box(
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
                    .padding(
                        start = if (margenLateral) MargenContenido else 0.dp,
                        end = if (margenLateral) MargenContenido else 0.dp,
                        top = 8.dp,
                        bottom = huecoInferior
                    )
                    .fillMaxSize()
            ) {
                contenido()
            }
        }
        val abajoDeLaBarra = huecoDeBarra + 16.dp
        if (accionFlotante != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.End
                        )
                    )
                    .padding(end = MargenContenido, bottom = abajoDeLaBarra)
            ) {
                accionFlotante()
            }
        }
        if (aviso != null) {
            HostDeAvisos(
                estado = aviso,
                modificador = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                    )
                    // Con botón flotante el aviso se coloca por encima de él.
                    .padding(
                        bottom = abajoDeLaBarra + if (accionFlotante != null) 80.dp else 0.dp
                    )
            )
        }
    }
}
