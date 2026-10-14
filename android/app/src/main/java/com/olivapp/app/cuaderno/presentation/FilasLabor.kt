package com.olivapp.app.cuaderno.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.olivapp.app.R
import com.olivapp.app.core.ui.BotonPeligro
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.cuaderno.domain.DatosLabor

/** Una fila de "Mi cuaderno": fecha y categoría, con sus campos legibles de un vistazo. */
@Composable
fun FilaLabor(
    labor: DatosLabor,
    alEditar: (() -> Unit)? = null,
    alEliminar: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = iconoDeCategoria(labor.categoria)),
                contentDescription = null,
                tint = Primario,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = labor.fecha + " · " + labor.categoria,
                style = TextoCuerpo,
                fontWeight = FontWeight.Bold,
                color = Texto
            )
        }
        if (labor.parcela.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            FilaCampoLabor(etiqueta = "Parcela", valor = labor.parcela)
        }
        for ((etiqueta, valor) in labor.campos) {
            Spacer(modifier = Modifier.height(4.dp))
            FilaCampoLabor(etiqueta = etiqueta, valor = valor)
        }
        if (labor.observaciones.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = labor.observaciones,
                style = TextoCuerpo,
                color = Texto
            )
        }
        if (alEditar != null) {
            Spacer(modifier = Modifier.height(12.dp))
            BotonSecundario(texto = "Editar", icono = R.drawable.ic_editar, alPulsar = alEditar)
        }
        if (alEliminar != null) {
            Spacer(modifier = Modifier.height(8.dp))
            BotonPeligro(texto = "Eliminar", icono = R.drawable.ic_eliminar, alPulsar = alEliminar)
        }
    }
}

/** Un campo tipo SIEX de la labor: etiqueta en negrita y su valor. */
@Composable
fun FilaCampoLabor(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$etiqueta: ",
            style = TextoCuerpo,
            fontWeight = FontWeight.Bold,
            color = Texto
        )
        Text(
            text = valor,
            style = TextoCuerpo,
            color = Texto
        )
    }
}

/**
 * Icono de la categoría de la labor. El icono acompaña al texto de la
 * categoría, nunca lo sustituye (regla de usabilidad sénior de DESIGN.md).
 */
private fun iconoDeCategoria(categoria: String): Int = when (categoria.lowercase()) {
    "tratamiento" -> R.drawable.ic_tratamiento
    "riego" -> R.drawable.ic_riego
    "cosecha" -> R.drawable.ic_cosecha
    else -> R.drawable.ic_hoja
}
