package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.olivapp.app.R

/**
 * Tarjeta de dato con variación: etiqueta, cifra destacada y la comparación
 * con la cifra anterior ("+48 %"), con icono además de color (sube o baja),
 * porque el color nunca es la única señal.
 */
@Composable
fun TarjetaDatoVariacion(
    etiqueta: String,
    valor: String,
    variacion: String? = null,
    sube: Boolean? = null,
    referencia: String? = null,
    modificador: Modifier = Modifier
) {
    Tarjeta(modificador = modificador) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = etiqueta,
                    style = TextoCuerpo,
                    color = TextoSecundario
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = valor,
                    style = TextoCifra,
                    color = Texto
                )
            }
            if (variacion != null && sube != null) {
                PildoraDeVariacion(texto = variacion, sube = sube)
            }
        }
        if (referencia != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = referencia,
                style = TextoEtiqueta,
                color = TextoSecundario
            )
        }
    }
}

/** Píldora de variación con icono de subida o bajada, además del color. */
@Composable
private fun PildoraDeVariacion(texto: String, sube: Boolean) {
    val fondo = if (sube) ChipVerdeFondo else ChipRojoFondo
    val colorTexto = if (sube) ChipVerdeTexto else ChipRojoTexto
    val icono = if (sube) R.drawable.ic_subida else R.drawable.ic_bajada
    val descripcion = if (sube) "Sube" else "Baja"
    Surface(
        shape = FormasOlivApp.extraLarge,
        color = fondo
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icono),
                contentDescription = descripcion,
                tint = colorTexto,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = texto,
                style = TextoEtiqueta,
                color = colorTexto
            )
        }
    }
}
