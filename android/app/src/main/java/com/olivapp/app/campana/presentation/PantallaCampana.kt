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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.cuaderno.presentation.FilaLabor
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.campana.domain.CampanasRepositorio
import com.olivapp.app.campana.domain.DatosCampana
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.Fondo
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoTitulo1
import com.olivapp.app.core.ui.TextoValor
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
    viewModel: CampanaViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Llamada autenticada: el token guardado acompaña a la petición.
    LaunchedEffect(Unit) {
        viewModel.cargar(campanaId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        estado.datos?.let {
            Text(
                text = "Campaña ${it.nombre}",
                style = TextoTitulo1,
                color = Texto
            )
            if (it.estado == "cerrada") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Campaña cerrada: solo consulta.\nNo se pueden añadir labores.",
                    style = TextoCuerpo,
                    color = Texto,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Datos oficiales de la cooperativa",
                style = TextoValor,
                color = Primario
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Primario, RoundedCornerShape(12.dp))
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
                style = TextoValor,
                color = Primario
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Primario, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (it.labores.isEmpty()) {
                    Text(
                        text = "Todavía no has apuntado labores en esta campaña.",
                        style = TextoCuerpo,
                        color = Texto,
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
            style = TextoCuerpo,
            color = Texto
        )
        estado.mensaje?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                style = TextoCuerpo,
                color = Error,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        BotonSecundario(texto = "Volver al panel", alPulsar = onVolver)
    }
}
