package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Campo de formulario grande: etiqueta encima (18sp), 64dp mínimo de alto y
 * unidad opcional dentro del campo ("kg", "%") a la derecha del valor.
 */
@Composable
fun CampoDeTexto(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    tipo: KeyboardType,
    oculto: Boolean = false,
    sufijo: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta, style = TextoCuerpo) },
        singleLine = true,
        visualTransformation = if (oculto) PasswordVisualTransformation() else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = tipo),
        textStyle = TextoCuerpo.copy(color = Texto),
        shape = FormasOlivApp.medium,
        suffix = if (sufijo == null) {
            null
        } else {
            { Text(text = sufijo, style = TextoCuerpo, color = TextoSecundario) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
    )
}
