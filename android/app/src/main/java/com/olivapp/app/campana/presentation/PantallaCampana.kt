package com.olivapp.app.campana.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.FilaLabor
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.campana.domain.CampanasRepositorio
import com.olivapp.app.campana.domain.DatosCampana
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.FondoCrema
import com.olivapp.app.core.ui.TextoError
import com.olivapp.app.core.ui.TextoMarron
import com.olivapp.app.core.ui.VerdeOliva
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Estado visible del detalle en consulta de una campaña. */
data class CampanaUiState(
    val datos: DatosCampana? = null,
    val mensaje: String? = null
)

@HiltViewModel
class CampanaViewModel @Inject constructor(
    private val sesion: SesionRepositorio,
    private val campanasRepo: CampanasRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampanaUiState())
    val uiState: StateFlow<CampanaUiState> = _uiState.asStateFlow()

    /** Carga la campaña pedida con el token guardado (a cada entrada). */
    fun cargar(campanaId: String) {
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.")
            }
        } else {
            campanasRepo.cargarCampana(token, campanaId) { cargada, texto ->
                _uiState.update { it.copy(datos = cargada, mensaje = texto) }
            }
        }
    }
}

@Composable
fun PantallaCampana(
    campanaId: String,
    onVolver: () -> Unit,
    visita: Int = 0,
    viewModel: CampanaViewModel = hiltViewModel(key = "visita-$visita")
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Llamada autenticada: el token guardado acompaña a la petición.
    LaunchedEffect(Unit) {
        viewModel.cargar(campanaId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        estado.datos?.let {
            Text(
                text = "Campaña ${it.nombre}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextoMarron
            )
            if (it.estado == "cerrada") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Campaña cerrada: solo consulta.\nNo se pueden añadir labores.",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoMarron,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Datos oficiales de la cooperativa",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                FilaDato(etiqueta = "Kilos entregados", valor = it.kilosEntregados)
                Spacer(modifier = Modifier.height(12.dp))
                FilaDato(etiqueta = "Rendimiento graso", valor = it.rendimientoGraso)
                Spacer(modifier = Modifier.height(12.dp))
                FilaDato(etiqueta = "Aceite obtenido", valor = it.aceiteObtenido)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Mi cuaderno",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (it.labores.isEmpty()) {
                    Text(
                        text = "Todavía no has apuntado labores en esta campaña.",
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = TextoMarron,
                        textAlign = TextAlign.Center
                    )
                } else {
                    for ((indice, labor) in it.labores.withIndex()) {
                        if (indice > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        FilaLabor(labor)
                    }
                }
            }
        } ?: Text(
            text = "Cargando los datos de la campaña…",
            fontSize = 18.sp,
            color = TextoMarron
        )
        estado.mensaje?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoError,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        BotonSecundario(texto = "Volver al panel", alPulsar = onVolver)
    }
}
