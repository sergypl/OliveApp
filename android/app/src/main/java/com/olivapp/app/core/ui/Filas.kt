package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Una fila del bloque de datos oficiales: etiqueta grande y valor destacado,
 * con icono opcional (siempre acompañado del texto de la etiqueta).
 */
@Composable
fun FilaDato(etiqueta: String, valor: String, icono: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Icon(
                painter = painterResource(id = icono),
                contentDescription = null,
                tint = Primario,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = etiqueta,
            style = TextoCuerpo,
            color = TextoSecundario,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valor,
            style = TextoValor,
            color = Texto
        )
    }
}

/**
 * Variante de [FilaDato] para valores largos (el nombre de la cooperativa, por
 * ejemplo): la etiqueta va arriba y el valor debajo, con todo el ancho. Así
 * ninguno de los dos se queda sin hueco ni estira la fila.
 */
@Composable
fun FilaDatoApilada(etiqueta: String, valor: String, icono: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (icono != null) {
            Icon(
                painter = painterResource(id = icono),
                contentDescription = null,
                tint = Primario,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = etiqueta, style = TextoCuerpo, color = TextoSecundario)
            Text(text = valor, style = TextoValor, color = Texto)
        }
    }
}
