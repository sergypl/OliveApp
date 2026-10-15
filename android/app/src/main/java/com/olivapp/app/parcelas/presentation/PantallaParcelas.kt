package com.olivapp.app.parcelas.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.EsqueletoDeCarga
import com.olivapp.app.core.ui.EstadoVacio
import com.olivapp.app.core.ui.PantallaConBarraSuperior
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.parcelas.domain.DatosParcela
import com.olivapp.app.parcelas.domain.ParcelasRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.olivapp.app.R

/** Estado visible de la pantalla de parcelas. */
data class ParcelasUiState(
    val nombre: String = "",
    val parcelas: List<DatosParcela> = emptyList(),
    val mensaje: String? = null,
    val creada: Boolean = false,
    val trabajando: Boolean = false,
    val cargando: Boolean = true,
    /** La carga de la lista falló: no se sabe si hay parcelas, así que no se dice que no hay. */
    val errorDeCarga: Boolean = false
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
        _uiState.update { it.copy(cargando = true) }
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(
                    cargando = false,
                    mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
                )
            }
            return
        }
        parcelasRepo.cargarParcelas(token) { lista, texto ->
            _uiState.update {
                if (lista != null) {
                    it.copy(
                        cargando = false,
                        parcelas = lista,
                        errorDeCarga = false,
                        // El aviso de una carga fallida no sobrevive a la carga buena;
                        // el de "parcela creada" sí.
                        mensaje = if (it.errorDeCarga) null else it.mensaje
                    )
                } else {
                    it.copy(cargando = false, mensaje = texto, errorDeCarga = true)
                }
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
 * Pantalla de parcelas del socio (features alta-de-parcela y
 * cuaderno-linea-de-tiempo, que le pone los componentes de la oleada 0): alta
 * con nombre propio y lista, con barra superior grande, esqueleto al cargar y
 * estado vacío.
 *
 * Las parcelas no se eliminan ni archivan en el MVP (docs/domain-model.md).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaParcelas(
    viewModel: ParcelasViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    PantallaConBarraSuperior(
        titulo = "Tus parcelas"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Aquí viven tus parcelas.\nLes pondrás labores desde el cuaderno.",
                style = TextoCuerpo,
                color = Texto,
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
                icono = R.drawable.ic_parcelas,
                habilitado = !estado.trabajando,
                alPulsar = viewModel::crearParcela
            )
            Spacer(modifier = Modifier.height(16.dp))
            estado.mensaje?.let {
                Text(
                    text = it,
                    style = TextoCuerpo,
                    color = if (estado.creada) Primario else Error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            when {
                estado.cargando -> EsqueletoDeCarga(
                    filas = 3,
                    altoFila = 28.dp,
                    modificador = Modifier.fillMaxWidth()
                )
                estado.errorDeCarga && estado.parcelas.isEmpty() -> BotonSecundario(
                    texto = "Reintentar",
                    icono = R.drawable.ic_refrescar,
                    alPulsar = viewModel::cargar
                )
                estado.parcelas.isEmpty() -> EstadoVacio(
                    titulo = "Aún no tienes parcelas. Añade la primera",
                    modificador = Modifier.padding(vertical = 8.dp)
                )
                else -> {
                    Text(
                        text = "Parcelas de este socio",
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
                        for ((indice, parcela) in estado.parcelas.withIndex()) {
                            if (indice > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = parcela.nombre,
                                style = TextoCuerpo,
                                color = Texto
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Las parcelas no se borran en esta versión.",
                style = TextoCuerpo,
                color = Texto,
                textAlign = TextAlign.Center
            )
        }
    }
}
