package com.olivapp.app.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Campo de formulario grande: etiqueta encima (18sp), 64dp mínimo de alto y
 * unidad opcional dentro del campo ("kg", "%") a la derecha del valor.
 *
 * Con `formato` (p. ej. millares con punto) el texto se reformatea al teclear y
 * el cursor se recoloca contando los dígitos ya escritos: sin esto, al insertarse
 * el punto de millar el cursor se quedaba un sitio atrás y la tecla siguiente
 * entraba antes del último dígito ("1.095" + "8" daba "10.985").
 */
@Composable
fun CampoDeTexto(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    tipo: KeyboardType,
    oculto: Boolean = false,
    sufijo: String? = null,
    formato: ((String) -> String)? = null
) {
    var campo by remember { mutableStateOf(TextFieldValue(valor, TextRange(valor.length))) }
    // El valor también cambia desde fuera (deslizador, botones de paso): se sincroniza.
    if (campo.text != valor) {
        campo = TextFieldValue(valor, TextRange(valor.length))
    }
    OutlinedTextField(
        value = campo,
        onValueChange = { nuevo ->
            if (formato == null) {
                campo = nuevo
                alCambiar(nuevo.text)
            } else {
                val formateado = formato(nuevo.text)
                campo = TextFieldValue(
                    formateado,
                    TextRange(posicionTrasFormato(nuevo.text, nuevo.selection.end, formateado))
                )
                alCambiar(formateado)
            }
        },
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

/**
 * Dónde queda el cursor tras reformatear un número. Al teclear al final, se
 * queda al final; si se edita en medio, se coloca tras los mismos dígitos (y
 * comas) que había a su izquierda, ignorando los puntos de millar que se
 * añaden o se quitan.
 */
internal fun posicionTrasFormato(textoNuevo: String, cursor: Int, formateado: String): Int {
    if (cursor >= textoNuevo.length) return formateado.length
    val objetivo = textoNuevo.take(cursor).count { it.isDigit() || it == ',' }
    if (objetivo == 0) return 0
    var vistos = 0
    for ((indice, caracter) in formateado.withIndex()) {
        if (caracter.isDigit() || caracter == ',') {
            vistos++
            if (vistos == objetivo) return indice + 1
        }
    }
    return formateado.length
}
