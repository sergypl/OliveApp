package com.olivapp.app.parcelas.presentation

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.FondoCrema
import com.olivapp.app.core.ui.TextoError
import com.olivapp.app.core.ui.TextoMarron
import com.olivapp.app.core.ui.VerdeOliva
import com.olivapp.app.parcelas.domain.DatosParcela
import com.olivapp.app.parcelas.domain.ParcelasRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Estado visible de la pantalla de parcelas. */
data class ParcelasUiState(
    val nombre: String = "",
    val parcelas: List<DatosParcela> = emptyList(),
    val mensaje: String? = null,
    val creada: Boolean = false,
    val trabajando: Boolean = false
)

@HiltViewModel
class ParcelasViewModel @Inject constructor(
    private val sesion: SesionRepositorio,
    private val parcelasRepo: ParcelasRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParcelasUiState())
    val uiState: StateFlow<ParcelasUiState> = _uiState.asStateFlow()

    fun cambiarNombre(valor: String) {
        _uiState.update { it.copy(nombre = valor) }
    }

    /** Carga la lista con el token guardado (a cada entrada en la pantalla). */
    fun cargar() {
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.")
            }
            return
        }
        parcelasRepo.cargarParcelas(token) { lista, texto ->
            _uiState.update {
                if (lista != null) it.copy(parcelas = lista) else it.copy(mensaje = texto)
            }
        }
    }

    fun crearParcela() {
        val estado = _uiState.value
        if (estado.trabajando) return
        _uiState.update { it.copy(trabajando = true, mensaje = null, creada = false) }
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(
                    trabajando = false,
                    mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
                )
            }
            return
        }
        parcelasRepo.crearParcela(token, estado.nombre) { exitosa, texto ->
            _uiState.update { it.copy(trabajando = false, creada = exitosa, mensaje = texto) }
            if (exitosa) {
                _uiState.update { it.copy(nombre = "") }
                cargar()
            }
        }
    }
}

/**
 * Pantalla de parcelas del socio: alta con nombre propio y lista.
 *
 * Las parcelas no se eliminan ni archivan en el MVP (docs/domain-model.md).
 */
@Composable
fun PantallaParcelas(
    onVolver: () -> Unit,
    visita: Int = 0,
    viewModel: ParcelasViewModel = hiltViewModel(key = "visita-$visita")
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tus parcelas",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aquí viven tus parcelas.\nLes pondrás labores desde el cuaderno.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CampoDeTexto(
            etiqueta = "Nombre de la parcela",
            valor = estado.nombre,
            alCambiar = viewModel::cambiarNombre,
            tipo = KeyboardType.Text
        )
        Spacer(modifier = Modifier.height(16.dp))
        BotonPrincipal(
            texto = if (estado.trabajando) "Creando la parcela…" else "Crear parcela",
            habilitado = !estado.trabajando,
            alPulsar = viewModel::crearParcela
        )
        Spacer(modifier = Modifier.height(16.dp))
        estado.mensaje?.let {
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = if (estado.creada) VerdeOliva else TextoError,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (estado.parcelas.isNotEmpty()) {
            Text(
                text = "Parcelas de este socio",
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
                for ((indice, parcela) in estado.parcelas.withIndex()) {
                    if (indice > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        text = parcela.nombre,
                        fontSize = 18.sp,
                        color = TextoMarron
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = "Las parcelas no se borran en esta versión.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonSecundario(texto = "Volver al panel", alPulsar = onVolver)
    }
}
