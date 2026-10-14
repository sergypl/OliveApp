package com.olivapp.app.cuaderno.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivapp.app.core.ui.BotonPeligro
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.TextoMarron
import com.olivapp.app.cuaderno.domain.DatosLabor

/** Una fila de "Mi cuaderno": fecha y categoría, con sus campos legibles de un vistazo. */
@Composable
fun FilaLabor(
    labor: DatosLabor,
    alEditar: (() -> Unit)? = null,
    alEliminar: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = labor.fecha + " · " + labor.categoria,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
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
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron
            )
        }
        if (alEditar != null) {
            Spacer(modifier = Modifier.height(12.dp))
            BotonSecundario(texto = "Editar", alPulsar = alEditar)
        }
        if (alEliminar != null) {
            Spacer(modifier = Modifier.height(8.dp))
            BotonPeligro(texto = "Eliminar", alPulsar = alEliminar)
        }
    }
}

/** Un campo tipo SIEX de la labor: etiqueta en negrita y su valor. */
@Composable
fun FilaCampoLabor(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$etiqueta: ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Text(
            text = valor,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron
        )
    }
}
