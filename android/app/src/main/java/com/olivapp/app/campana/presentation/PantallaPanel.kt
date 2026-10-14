package com.olivapp.app.campana.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.campana.domain.CampanasRepositorio
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.DatosPanel
import com.olivapp.app.core.ui.BotonAccion
import com.olivapp.app.core.ui.BotonMicrofono
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CabeceraSeccion
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.Fondo
import com.olivapp.app.core.ui.PanelDestacado
import com.olivapp.app.core.ui.Tarjeta
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.TextoSecundario
import com.olivapp.app.core.ui.TextoTitulo1
import com.olivapp.app.core.ui.TextoTitulo2
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.core.ui.pantallaCompleta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.olivapp.app.R

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
            .pantallaCompleta()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        estado.datos?.let {
            Tarjeta {
                Text(
                    text = "Campaña ${it.campana}",
                    style = TextoTitulo1,
                    color = Texto
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Los datos oficiales llevan cabecera y superficie propias para
            // no confundirlos con las labores del socio (regla de DESIGN.md).
            CabeceraSeccion(icono = R.drawable.ic_cooperativa, titulo = "Datos oficiales de la cooperativa")
            Tarjeta {
                FilaDato(etiqueta = "Kilos entregados", valor = it.kilosEntregados, icono = R.drawable.ic_kilos)
                Spacer(modifier = Modifier.height(12.dp))
                FilaDato(etiqueta = "Rendimiento graso", valor = it.rendimientoGraso, icono = R.drawable.ic_rendimiento)
                Spacer(modifier = Modifier.height(12.dp))
                FilaDato(etiqueta = "Aceite obtenido", valor = it.aceiteObtenido, icono = R.drawable.ic_aceite)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Estos son los datos que declara la cooperativa.\nTus labores van aparte, en el cuaderno de campo.",
                style = TextoCuerpo,
                color = TextoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } ?: Text(
            text = "Cargando tus datos…",
            style = TextoCuerpo,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        estado.mensaje?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                style = TextoCuerpo,
                color = Error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        val cerradas = estado.campanas.filter { it.estado == "cerrada" }
        if (cerradas.isNotEmpty()) {
            Text(
                text = "Campañas anteriores",
                style = TextoTitulo2,
                color = Texto,
                modifier = Modifier.fillMaxWidth()
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
            style = TextoTitulo2,
            color = Texto,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        // El micrófono es el protagonista del panel: naranja, circular, 96dp.
        PanelDestacado {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BotonMicrofono(
                    descripcion = "Dictar una nota de voz",
                    alPulsar = onAbrirNotasVoz
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Dictar una nota de voz",
                    style = TextoValor,
                    color = Texto,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Graba una nota corta para apuntar una labor.",
                    style = TextoCuerpo,
                    color = TextoSecundario,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        BotonAccion(texto = "Tus parcelas", icono = R.drawable.ic_parcelas, alPulsar = onAbrirParcelas)
        Spacer(modifier = Modifier.height(12.dp))
        BotonAccion(texto = "Cuaderno de campo", icono = R.drawable.ic_cuaderno, alPulsar = onAbrirCuaderno)
        Spacer(modifier = Modifier.height(12.dp))
        BotonAccion(
            texto = "Simulador de liquidación en limpio",
            icono = R.drawable.ic_simulador,
            alPulsar = onAbrirSimulador
        )
        Spacer(modifier = Modifier.height(32.dp))
        BotonSecundario(
            texto = "Cerrar sesión",
            icono = R.drawable.ic_cerrar_sesion,
            alPulsar = viewModel::cerrarSesion
        )
    }
}
