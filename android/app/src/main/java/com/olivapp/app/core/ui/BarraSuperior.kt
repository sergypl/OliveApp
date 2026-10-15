package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.olivapp.app.R

/**
 * Pantalla con barra superior grande (Material 3) que se encoge al hacer
 * scroll y lleva botón de volver (solo si se pasa `onVolver`: las pestañas de
 * primer nivel no lo llevan), siempre con icono Y texto accesible.
 *
 * El contenido se dibuja dentro del scroll que anima la barra; quien llama no
 * tiene que preocuparse de las barras del sistema (van incluidas en el
 * padding del Scaffold) ni del hueco de la futura barra inferior. El hueco se
 * retira con el teclado abierto para no desperdiciar espacio.
 *
 * El hueco `fijo` (opcional) queda FUERA del scroll, entre la barra y el
 * contenido: sirve para lo que no se debe ir de pantalla, como la tarjeta de
 * resultado del simulador.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaConBarraSuperiorGrande(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    reservaBarraInferior: Boolean = true,
    comportamientoBarra: TopAppBarScrollBehavior? = null,
    fijo: (@Composable () -> Unit)? = null,
    contenido: @Composable (PaddingValues) -> Unit
) {
    val estadoBarra = rememberTopAppBarState()
    val comportamiento = comportamientoBarra
        ?: TopAppBarDefaults.enterAlwaysScrollBehavior(estadoBarra)
    val huecoInferior =
        if (reservaBarraInferior && LocalBarraInferiorVisible.current && !WindowInsets.isImeVisible) {
            AlturaBarraInferior
        } else {
            24.dp
        }
    Scaffold(
        containerColor = Fondo,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { BarraSuperiorGrande(titulo, onVolver, comportamiento) }
    ) { margenes ->
        Column(
            modifier = Modifier
                .padding(margenes)
                .padding(horizontal = 24.dp)
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
                contenido(PaddingValues(0.dp))
            }
        }
    }
}

/**
 * Variante de la barra superior grande para pantallas con lista propia (por
 * ejemplo la línea de tiempo de "Mi cuaderno"): el contenido es una lista que
 * maneja su propio scroll y puede traer cabeceras fijas, aviso con "Deshacer"
 * y botón flotante. El hueco de la barra inferior se reserva aquí para que la
 * lista se dibuje por encima sin que la tapen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConBarraSuperiorGrandeEnLista(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    reservaBarraInferior: Boolean = true,
    aviso: SnackbarHostState? = null,
    accionFlotante: (@Composable () -> Unit)? = null,
    contenido: @Composable () -> Unit
) {
    val estadoBarra = rememberTopAppBarState()
    val comportamiento = TopAppBarDefaults.enterAlwaysScrollBehavior(estadoBarra)
    Scaffold(
        containerColor = Fondo,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { BarraSuperiorGrande(titulo, onVolver, comportamiento) },
        snackbarHost = { if (aviso != null) HostDeAvisos(aviso) },
        floatingActionButton = { accionFlotante?.invoke() }
    ) { margenes ->
        Box(
            modifier = Modifier
                .padding(margenes)
                .padding(bottom = if (reservaBarraInferior) AlturaBarraInferior else 24.dp)
                .fillMaxSize()
        ) {
            contenido()
        }
    }
}

/** La barra superior grande compartida por las dos variantes de pantalla. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSuperiorGrande(
    titulo: String,
    onVolver: (() -> Unit)?,
    comportamiento: TopAppBarScrollBehavior
) {
    LargeTopAppBar(
        title = { Text(text = titulo, style = TextoTitulo1, color = Texto) },
        navigationIcon = {
            // Las pestañas de primer nivel no llevan flecha de volver.
            if (onVolver != null) {
                IconButton(onClick = onVolver) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_volver),
                        contentDescription = "Volver",
                        tint = Primario
                    )
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Fondo,
            scrolledContainerColor = ContenedorBajo,
            titleContentColor = Texto,
            navigationIconContentColor = Primario
        ),
        // Sin flecha de volver (pestañas) la barra no necesita 152dp: la franja
        // vacía encima del título era espacio perdido.
        expandedHeight = if (onVolver == null) {
            96.dp
        } else {
            TopAppBarDefaults.LargeAppBarExpandedHeight
        },
        scrollBehavior = comportamiento
    )
}
