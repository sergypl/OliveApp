package com.olivapp.app.core.navegacion

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.olivapp.app.core.ui.ContenedorBajo
import com.olivapp.app.core.ui.ContenedorMasAlto
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoEtiqueta
import com.olivapp.app.core.ui.TextoSecundario

/** Tope del tamaño de letra de la barra: cinco etiquetas deben caber en una línea. */
private const val ESCALA_MAXIMA_DE_LETRA_EN_BARRA = 1.2f

/**
 * Barra de navegación inferior (Material 3, CONTEXT.md: "Barra de
 * navegación"). Fija, con etiqueta siempre visible en cada pestaña y la
 * pestaña activa marcada con indicador, icono en verde oliva y etiqueta en
 * negrita (no depende solo del color). Cada pestaña mide 80dp de alto (M3).
 */
@Composable
fun BarraInferior(
    activa: Pestana,
    alElegir: (Pestana) -> Unit,
    modifier: Modifier = Modifier
) {
    val densidad = LocalDensity.current
    val densidadAcotada = Density(
        density = densidad.density,
        fontScale = densidad.fontScale.coerceAtMost(ESCALA_MAXIMA_DE_LETRA_EN_BARRA)
    )
    CompositionLocalProvider(LocalDensity provides densidadAcotada) {
        NavigationBar(
            modifier = modifier.fillMaxWidth(),
            containerColor = ContenedorBajo,
            contentColor = Texto
        ) {
            for (pestana in Pestana.entries) {
                val seleccionada = pestana == activa
                NavigationBarItem(
                    selected = seleccionada,
                    onClick = { alElegir(pestana) },
                    icon = {
                        Icon(
                            painter = painterResource(id = pestana.icono),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = pestana.etiqueta,
                            style = TextoEtiqueta,
                            fontSize = 14.sp,
                            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primario,
                        selectedTextColor = Texto,
                        indicatorColor = ContenedorMasAlto,
                        unselectedIconColor = TextoSecundario,
                        unselectedTextColor = TextoSecundario
                    )
                )
            }
        }
    }
}
