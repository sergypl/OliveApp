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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.campana.domain.CampanasRepositorio
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.DatosPanel
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

/** Estado visible del panel de la campaña actual. */
data class PanelUiState(
    val datos: DatosPanel? = null,
    val campanas: List<DatosCampanaResumen> = emptyList(),
    val mensaje: String? = null,
    val sesionCerrada: Boolean = false
)

@HiltViewModel
class PanelViewModel @Inject constructor(
    private val sesion: SesionRepositorio,
    private val campanasRepo: CampanasRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(PanelUiState())
    val uiState: StateFlow<PanelUiState> = _uiState.asStateFlow()

    /**
     * Carga los datos oficiales y las campañas con el token guardado. Se llama
     * a cada entrada en la pantalla (como hacía el `remember` original).
     */
    fun cargar() {
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.")
            }
        } else {
            campanasRepo.cargarMiPanel(token) { cargados, texto ->
                _uiState.update { it.copy(datos = cargados, mensaje = texto) }
            }
            campanasRepo.cargarCampanas(token) { lista, texto ->
                _uiState.update {
                    if (lista != null) {
                        it.copy(campanas = lista)
                    } else {
                        it.copy(mensaje = texto)
                    }
                }
            }
        }
    }

    fun cerrarSesion() {
        // Se borra el token del dispositivo: sin él no hay acceso.
        sesion.borrar()
        _uiState.update { it.copy(sesionCerrada = true) }
    }

    /** La navegación tras cerrar sesión ya se ha hecho. */
    fun sesionCerradaAtendida() {
        _uiState.update { it.copy(sesionCerrada = false) }
    }
}

@Composable
fun PantallaPanel(
    onSesionCerrada: () -> Unit,
    onAbrirParcelas: () -> Unit,
    onAbrirCuaderno: () -> Unit,
    onAbrirNotasVoz: () -> Unit,
    onAbrirSimulador: () -> Unit,
    onAbrirCampana: (String) -> Unit,
    viewModel: PanelViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Carga autenticada: el token guardado acompaña a las peticiones.
    LaunchedEffect(Unit) {
        viewModel.cargar()
    }
    LaunchedEffect(estado.sesionCerrada) {
        if (estado.sesionCerrada) {
            viewModel.sesionCerradaAtendida()
            onSesionCerrada()
        }
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
                text = "Campaña ${it.campana}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextoMarron
            )
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Estos son los datos que declara la cooperativa.\nTus labores van aparte, en el cuaderno de campo.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron,
                textAlign = TextAlign.Center
            )
        } ?: Text(
            text = "Cargando tus datos…",
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
        val cerradas = estado.campanas.filter { it.estado == "cerrada" }
        if (cerradas.isNotEmpty()) {
            Text(
                text = "Campañas anteriores",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(12.dp))
            for (c in cerradas) {
                BotonSecundario(
                    texto = "Campaña ${c.nombre}",
                    alPulsar = { onAbrirCampana(c.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(
            text = "Tus herramientas",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeOliva
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Botón grande de micrófono del panel: grabar una nota de voz.
        Button(
            onClick = onAbrirNotasVoz,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VerdeOliva,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Dictar una nota de voz",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Graba una nota corta para apuntar una labor.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        BotonSecundario(texto = "Tus parcelas", alPulsar = onAbrirParcelas)
        Spacer(modifier = Modifier.height(12.dp))
        BotonSecundario(texto = "Cuaderno de campo", alPulsar = onAbrirCuaderno)
        Spacer(modifier = Modifier.height(12.dp))
        BotonSecundario(
            texto = "Simulador de liquidación en limpio",
            alPulsar = onAbrirSimulador
        )
        Spacer(modifier = Modifier.height(32.dp))
        BotonSecundario(
            texto = "Cerrar sesión",
            alPulsar = viewModel::cerrarSesion
        )
    }
}
