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
import androidx.compose.ui.unit.sp
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.TextoError
import com.olivapp.app.core.ui.TextoMarron
import com.olivapp.app.core.ui.VerdeOliva
import com.olivapp.app.voz.data.AlmacenNotasVoz

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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = nota.estadoVisible,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = if (nota.estado == "error" || nota.estado == "audio_invalido") {
                TextoError
            } else {
                VerdeOliva
            }
        )
        if (nota.texto.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nota.texto,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron
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
            BotonSecundario(texto = "Reintentar", alPulsar = alReintentar)
        }
        if (nota.estado == "audio_invalido") {
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(texto = "Grabar otra vez", alPulsar = alRepetir)
        }
    }
}
