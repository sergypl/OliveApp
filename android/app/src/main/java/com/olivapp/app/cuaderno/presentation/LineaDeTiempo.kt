package com.olivapp.app.cuaderno.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olivapp.app.core.formatos.NOMBRES_DE_CATEGORIA
import com.olivapp.app.core.ui.ChipFiltro
import com.olivapp.app.core.ui.ContenedorBajo
import com.olivapp.app.core.ui.FormasOlivApp
import com.olivapp.app.core.ui.MargenContenido
import com.olivapp.app.core.ui.Superficie
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.core.ui.Borde
import com.olivapp.app.core.ui.margenDeContenido
import com.olivapp.app.cuaderno.domain.DatosLabor
import com.olivapp.app.cuaderno.domain.FiltroDeCuaderno
import com.olivapp.app.cuaderno.domain.GrupoDeMes
import com.olivapp.app.cuaderno.domain.OpcionesDeFiltro

/**
 * Piezas de la línea de tiempo de "Mi cuaderno" (feature
 * cuaderno-linea-de-tiempo): cabecera de mes para las cabeceras fijas, tarjeta
 * de labor con el color de su categoría y chips de filtro grandes.
 */

/** Cabecera de mes de la línea de tiempo: se queda fija mientras se ve su mes. */
@Composable
fun CabeceraDeMes(grupo: GrupoDeMes) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ContenedorBajo,
        shape = FormasOlivApp.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = grupo.titulo,
                style = TextoValor,
                color = Texto,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (grupo.labores.size == 1) "1 labor" else "${grupo.labores.size} labores",
                style = TextoCuerpo,
                color = Texto
            )
        }
    }
}

/**
 * Tarjeta de una labor en la línea de tiempo: barra de color de la categoría a
 * la izquierda y, dentro, la fila de siempre (icono, texto y campos).
 */
@Composable
fun TarjetaDeLabor(
    labor: DatosLabor,
    alEditar: (() -> Unit)? = null,
    alEliminar: (() -> Unit)? = null,
    modificador: Modifier = Modifier
) {
    Surface(
        modifier = modificador.fillMaxWidth(),
        shape = FormasOlivApp.large,
        color = Superficie,
        border = BorderStroke(1.dp, Borde)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(colorDeCategoria(labor.editable.categoria))
            )
            Column(modifier = Modifier.padding(16.dp)) {
                FilaLabor(labor, alEditar = alEditar, alEliminar = alEliminar)
            }
        }
    }
}

/**
 * Chips de filtro de la línea de tiempo, grandes y con texto: primero la
 * categoría y después la parcela. "Todas" siempre al principio y el chip
 * elegido se marca con el icono de verificación, además del color.
 */
@Composable
fun FiltrosDeCuaderno(
    opciones: OpcionesDeFiltro,
    filtro: FiltroDeCuaderno,
    alFiltrarCategoria: (String?) -> Unit,
    alFiltrarParcela: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Filtrar por categoría",
            style = TextoCuerpo,
            color = Texto,
            modifier = Modifier.margenDeContenido()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FilaDeChips {
            ChipFiltro(
                texto = "Todas",
                elegido = filtro.categoria == null,
                alPulsar = { alFiltrarCategoria(null) }
            )
            for (categoria in opciones.categorias) {
                Spacer(modifier = Modifier.width(12.dp))
                ChipFiltro(
                    texto = NOMBRES_DE_CATEGORIA[categoria] ?: categoria,
                    elegido = filtro.categoria == categoria,
                    icono = iconoDeCategoria(categoria),
                    alPulsar = {
                        alFiltrarCategoria(if (filtro.categoria == categoria) null else categoria)
                    }
                )
            }
        }
        if (opciones.parcelas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Filtrar por parcela",
                style = TextoCuerpo,
                color = Texto,
                modifier = Modifier.margenDeContenido()
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilaDeChips {
                ChipFiltro(
                    texto = "Todas",
                    elegido = filtro.parcela == null,
                    alPulsar = { alFiltrarParcela(null) }
                )
                for (parcela in opciones.parcelas) {
                    Spacer(modifier = Modifier.width(12.dp))
                    ChipFiltro(
                        texto = parcela,
                        elegido = filtro.parcela == parcela,
                        alPulsar = {
                            alFiltrarParcela(if (filtro.parcela == parcela) null else parcela)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Fila de chips que se desliza a los lados si no caben todos. Va a todo el
 * ancho de la pantalla (fuera del margen de contenido) para que los chips pasen
 * por debajo de los márgenes sin cortarse al deslizarlos; el margen va dentro
 * del área deslizante, de modo que el primer y el último chip quedan alineados
 * con el margen de contenido cuando la fila está en reposo.
 */
@Composable
private fun FilaDeChips(contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MargenContenido)
    ) {
        contenido()
    }
}
