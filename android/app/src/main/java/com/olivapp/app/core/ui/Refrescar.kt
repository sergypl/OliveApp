package com.olivapp.app.core.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Contenedor de "deslizar hacia abajo para refrescar": envuelve la lista de
 * una pantalla para que el socio pueda recargarla con el gesto de siempre,
 * con indicador visible y sin barras que se escondan al hacer scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenedorRefrescar(
    refrescando: Boolean,
    alRefrescar: () -> Unit,
    modificador: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    val estado = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refrescando,
        onRefresh = alRefrescar,
        state = estado,
        modifier = modificador,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = estado,
                isRefreshing = refrescando,
                containerColor = Contenedor,
                color = Primario,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        contenido()
    }
}
