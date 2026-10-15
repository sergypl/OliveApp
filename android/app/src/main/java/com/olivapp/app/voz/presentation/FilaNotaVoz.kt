package com.olivapp.app.voz.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.voz.data.AlmacenNotasVoz
import com.olivapp.app.R

/** Una nota de voz de la cola local: cuándo se grabó, cuánto dura, su estado y su texto. */
@Composable
fun FilaNotaVoz(
    nota: AlmacenNotasVoz.NotaDeVoz,
    alReintentar: () -> Unit,
    alRepetir: () -> Unit,
    alGuardarBorrador: (String) -> Unit,
    alConfirmarBorrador: () -> Unit,
    alDescartarBorrador: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = nota.cuando + " · " + nota.segundos + " s",
            style = TextoCuerpo,
            fontWeight = FontWeight.Bold,
            color = Texto
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = nota.estadoVisible,
            style = TextoCuerpo,
            color = if (nota.estado == "error" || nota.estado == "audio_invalido") {
                Error
            } else {
                Primario
            }
        )
        if (nota.texto.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nota.texto,
                style = TextoCuerpo,
                color = Texto
            )
        }
        if (nota.estado == "transcrita" && nota.borrador.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            BorradorIA(
                nota.borrador, nota.id, alGuardarBorrador, alConfirmarBorrador,
                alDescartarBorrador
            )
        }
        if (nota.estado == "error" || nota.estado == "pendiente") {
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(texto = "Reintentar", icono = R.drawable.ic_refrescar, alPulsar = alReintentar)
        }
        if (nota.estado == "audio_invalido") {
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(texto = "Grabar otra vez", icono = R.drawable.ic_mic, alPulsar = alRepetir)
        }
    }
}
