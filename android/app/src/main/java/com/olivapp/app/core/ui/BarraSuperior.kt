package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
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
 * scroll y lleva botón de volver, siempre con icono Y texto accesible.
 *
 * El contenido se dibuja dentro del scroll que anima la barra; quien llama no
 * tiene que preocuparse de las barras del sistema (van incluidas en el
 * padding del Scaffold) ni del hueco de la futura barra inferior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConBarraSuperiorGrande(
    titulo: String,
    onVolver: () -> Unit,
    reservaBarraInferior: Boolean = true,
    comportamientoBarra: TopAppBarScrollBehavior? = null,
    contenido: @Composable (PaddingValues) -> Unit
) {
    val estadoBarra = rememberTopAppBarState()
    val comportamiento = comportamientoBarra
        ?: TopAppBarDefaults.enterAlwaysScrollBehavior(estadoBarra)
    Scaffold(
        containerColor = Fondo,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = titulo, style = TextoTitulo1, color = Texto) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_volver),
                            contentDescription = "Volver",
                            tint = Primario
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Fondo,
                    scrolledContainerColor = ContenedorBajo,
                    titleContentColor = Texto,
                    navigationIconContentColor = Primario
                ),
                scrollBehavior = comportamiento
            )
        }
    ) { margenes ->
        Column(
            modifier = Modifier
                .padding(margenes)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 8.dp,
                    bottom = if (reservaBarraInferior && LocalBarraInferiorVisible.current) {
                        AlturaBarraInferior
                    } else {
                        24.dp
                    }
                )
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            contenido(PaddingValues(0.dp))
        }
    }
}
