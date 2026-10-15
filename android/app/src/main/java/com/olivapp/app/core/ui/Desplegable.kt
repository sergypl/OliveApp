package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Botón de categoría: relleno verde cuando está elegida (64dp, regla sénior). */
@Composable
fun OpcionCategoria(texto: String, icono: Int? = null, elegida: Boolean, alPulsar: () -> Unit) {
    if (elegida) {
        Button(
            onClick = alPulsar,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = FormasOlivApp.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primario,
                contentColor = SobrePrimario
            )
        ) {
            if (icono != null) {
                Icon(
                    painter = painterResource(id = icono),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text = texto, style = TextoCuerpo)
        }
    } else {
        OutlinedButton(
            onClick = alPulsar,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = FormasOlivApp.medium,
            border = androidx.compose.foundation.BorderStroke(2.dp, Primario)
        ) {
            if (icono != null) {
                Icon(
                    painter = painterResource(id = icono),
                    contentDescription = null,
                    tint = Primario,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text = texto, style = TextoCuerpo, color = Primario)
        }
    }
}

/** Lista desplegable grande y clara (parcela, plaga, unidad, modalidad). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Desplegable(
    etiqueta: String,
    valor: String,
    opciones: List<String>,
    alElegir: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta, style = TextoCuerpo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            textStyle = TextoCuerpo.copy(color = Texto),
            shape = FormasOlivApp.medium,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            for (opcion in opciones) {
                DropdownMenuItem(
                    text = { Text(opcion, style = TextoCuerpo) },
                    onClick = {
                        alElegir(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}
