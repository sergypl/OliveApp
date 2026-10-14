package com.olivapp.app.cuaderno.presentation

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.core.formatos.NOMBRES_DE_CATEGORIA
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.Fondo
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.TextoBoton
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoTitulo1
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.core.ui.pantallaCompleta
import com.olivapp.app.cuaderno.domain.DatosCampanaDeLabores
import com.olivapp.app.cuaderno.domain.LaboresRepositorio
import com.olivapp.app.cuaderno.domain.LaborEditable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.olivapp.app.R

/** Estado visible de "Mi cuaderno". */
data class CuadernoUiState(
    val campanas: List<DatosCampanaDeLabores> = emptyList(),
    val cargado: Boolean = false,
    val mensaje: String? = null,
    val laborParaEliminar: LaborEditable? = null
)

@HiltViewModel
class CuadernoViewModel @Inject constructor(
    private val sesion: SesionRepositorio,
    private val laboresRepo: LaboresRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(CuadernoUiState())
    val uiState: StateFlow<CuadernoUiState> = _uiState.asStateFlow()

    /** Carga el cuaderno con el token guardado (a cada entrada en la pantalla). */
    fun cargar() {
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(
                    mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.",
                    cargado = true
                )
            }
            return
        }
        laboresRepo.cargarMisLabores(token) { lista, texto ->
            _uiState.update {
                it.copy(
                    cargado = true,
                    campanas = lista ?: it.campanas,
                    mensaje = if (lista != null) it.mensaje else texto
                )
            }
        }
    }

    /** El socio pide eliminar una labor: se le pide confirmación antes. */
    fun pedirEliminar(labor: LaborEditable) {
        _uiState.update { it.copy(laborParaEliminar = labor) }
    }

    fun cancelarEliminar() {
        _uiState.update { it.copy(laborParaEliminar = null) }
    }

    /** Borrado directo confirmado (no hay papelera que lo recupere). */
    fun confirmarEliminar() {
        val objetivo = _uiState.value.laborParaEliminar ?: return
        _uiState.update { it.copy(laborParaEliminar = null) }
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.")
            }
        } else {
            laboresRepo.eliminarLabor(token, objetivo.id) { exito, texto ->
                _uiState.update { it.copy(mensaje = texto) }
                if (exito) {
                    cargar()
                }
            }
        }
    }
}

/**
 * Pantalla "Mi cuaderno" (feature consulta-cuaderno-por-campana).
 *
 * Historial cronológico de las labores del socio agrupado por campaña: de la
 * campaña más reciente a la más antigua y, dentro de cada campaña, de la
 * labor más reciente a la más antigua, con los campos tipo SIEX de cada una
 * legibles de un vistazo. Solo labores vigentes (sin papelera).
 */
@Composable
fun PantallaCuaderno(
    onVolver: () -> Unit,
    onApuntarLabor: () -> Unit,
    onEditarLabor: (LaborEditable) -> Unit,
    viewModel: CuadernoViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    // Confirmación antes del borrado directo (no hay papelera que lo recupere).
    estado.laborParaEliminar?.let { labor ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarEliminar,
            title = {
                Text(
                    text = "¿Eliminar esta labor?",
                    style = TextoValor,
                    color = Texto
                )
            },
            text = {
                Text(
                    text = labor.fechaVisible + " · " +
                        (NOMBRES_DE_CATEGORIA[labor.categoria] ?: labor.categoria) +
                        "\n\nSe borrará de tu cuaderno. No se puede deshacer.",
                    style = TextoCuerpo,
                    color = Texto
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarEliminar) {
                    Text(text = "Eliminar", style = TextoBoton, color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarEliminar) {
                    Text(text = "Cancelar", style = TextoBoton, color = Primario)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .pantallaCompleta()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mi cuaderno",
            style = TextoTitulo1,
            color = Texto
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Historial de tus labores por campaña,\nde la más reciente a la más antigua.",
            style = TextoCuerpo,
            color = Texto,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(texto = "Apuntar una labor", icono = R.drawable.ic_anadir, alPulsar = onApuntarLabor)
        Spacer(modifier = Modifier.height(24.dp))
        if (!estado.cargado) {
            Text(
                text = "Cargando tu cuaderno…",
                style = TextoCuerpo,
                color = Texto
            )
        } else if (estado.campanas.isEmpty() && estado.mensaje == null) {
            Text(
                text = "Todavía no has apuntado labores.",
                style = TextoCuerpo,
                color = Texto,
                textAlign = TextAlign.Center
            )
        } else {
            for ((indice, campana) in estado.campanas.withIndex()) {
                if (indice > 0) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Campaña " + campana.nombre +
                            if (campana.esActual) " · Campaña actual" else "",
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
                        for ((indiceLabor, labor) in campana.labores.withIndex()) {
                            if (indiceLabor > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            FilaLabor(
                                labor,
                                alEditar = { onEditarLabor(labor.editable) },
                                alEliminar = { viewModel.pedirEliminar(labor.editable) }
                            )
                        }
                    }
                }
            }
        }
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
        BotonSecundario(texto = "Volver al panel", icono = R.drawable.ic_volver, alPulsar = onVolver)
    }
}
