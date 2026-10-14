package com.olivapp.app.cuaderno.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.core.formatos.formatearCantidad
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.Desplegable
import com.olivapp.app.core.ui.Fondo
import com.olivapp.app.core.ui.OpcionCategoria
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoTitulo1
import com.olivapp.app.cuaderno.domain.LaboresRepositorio
import com.olivapp.app.cuaderno.domain.LaborEditable
import com.olivapp.app.cuaderno.domain.fechaDeHoy
import com.olivapp.app.cuaderno.domain.fechaIso
import com.olivapp.app.parcelas.domain.DatosParcela
import com.olivapp.app.parcelas.domain.ParcelasRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import javax.inject.Inject

/** Estado visible del formulario de una labor (alta nueva o edición). */
data class NuevaLaborUiState(
    val esEdicion: Boolean = false,
    val fecha: String = "",
    val parcelas: List<DatosParcela> = emptyList(),
    val parcelaId: String = "",
    val parcelaNombre: String = "",
    val categoria: String = "",
    val producto: String = "",
    val plaga: String = "",
    val cantidad: String = "",
    val unidad: String = "",
    val modalidad: String = "",
    val kilos: String = "",
    val observaciones: String = "",
    val mensaje: String? = null,
    val guardada: Boolean = false,
    val campanaGuardada: String? = null,
    val trabajando: Boolean = false
)

@HiltViewModel
class NuevaLaborViewModel @Inject constructor(
    private val sesion: SesionRepositorio,
    private val laboresRepo: LaboresRepositorio,
    private val parcelasRepo: ParcelasRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(NuevaLaborUiState())
    val uiState: StateFlow<NuevaLaborUiState> = _uiState.asStateFlow()

    private var laborEditada: LaborEditable? = null
    private var preparada = false

    /**
     * Prepara el formulario con la labor a editar (o en blanco para una alta
     * nueva) y carga las parcelas del socio. La primera llamada de la visita
     * manda: el estado sobrevive a la rotación.
     */
    fun preparar(labor: LaborEditable?) {
        if (preparada) return
        preparada = true
        laborEditada = labor
        _uiState.value = NuevaLaborUiState(
            esEdicion = labor != null,
            fecha = labor?.fechaVisible ?: fechaDeHoy(),
            parcelaId = labor?.parcelaId ?: "",
            parcelaNombre = labor?.parcelaNombre ?: "",
            categoria = labor?.categoria ?: "",
            producto = labor?.campos?.optString("producto") ?: "",
            plaga = labor?.campos?.optString("plaga_a_controlar") ?: "",
            cantidad = numeroInicial(labor, "cantidad"),
            unidad = labor?.campos?.optString("unidad") ?: "",
            modalidad = labor?.campos?.optString("modalidad") ?: "",
            kilos = numeroInicial(labor, "kilos"),
            observaciones = labor?.observaciones ?: ""
        )
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.")
            }
        } else {
            parcelasRepo.cargarParcelas(token) { lista, texto ->
                _uiState.update {
                    if (lista != null) it.copy(parcelas = lista) else it.copy(mensaje = texto)
                }
            }
        }
    }

    fun cambiarFecha(valor: String) {
        _uiState.update { it.copy(fecha = valor) }
    }

    fun elegirParcela(nombre: String) {
        _uiState.update {
            it.copy(
                parcelaNombre = nombre,
                parcelaId = it.parcelas.firstOrNull { p -> p.nombre == nombre }?.id ?: ""
            )
        }
    }

    fun elegirCategoria(valor: String) {
        // Al cambiar a tratamiento o riego, la unidad de la categoría anterior
        // no sirve: se vacía para que el socio elija la que corresponde.
        _uiState.update {
            when (valor) {
                "tratamiento", "riego" -> it.copy(categoria = valor, unidad = "")
                else -> it.copy(categoria = valor)
            }
        }
    }

    fun cambiarProducto(valor: String) {
        _uiState.update { it.copy(producto = valor) }
    }

    fun cambiarPlaga(valor: String) {
        _uiState.update { it.copy(plaga = valor) }
    }

    fun cambiarCantidad(valor: String) {
        _uiState.update { it.copy(cantidad = valor) }
    }

    fun elegirUnidad(valor: String) {
        _uiState.update { it.copy(unidad = valor) }
    }

    fun elegirModalidad(valor: String) {
        _uiState.update { it.copy(modalidad = valor) }
    }

    fun cambiarKilos(valor: String) {
        _uiState.update { it.copy(kilos = valor) }
    }

    fun cambiarObservaciones(valor: String) {
        _uiState.update { it.copy(observaciones = valor) }
    }

    /** Guarda la labor: alta nueva o edición de la que se está corrigiendo. */
    fun guardar() {
        val estado = _uiState.value
        if (estado.trabajando) return
        _uiState.update {
            it.copy(trabajando = true, mensaje = null, guardada = false, campanaGuardada = null)
        }
        val token = sesion.token()
        val iso = fechaIso(estado.fecha)
        if (token == null) {
            _uiState.update {
                it.copy(
                    trabajando = false,
                    mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
                )
            }
        } else if (iso == null) {
            _uiState.update {
                it.copy(
                    trabajando = false,
                    mensaje = "Escribe la fecha como día, mes y año (por ejemplo, 15/12/2025)."
                )
            }
        } else {
            val campos = JSONObject()
            when (estado.categoria) {
                "tratamiento" -> campos
                    .put("producto", estado.producto)
                    .put("plaga_a_controlar", estado.plaga)
                    .put("cantidad", estado.cantidad.replace(',', '.'))
                    .put("unidad", estado.unidad)
                "riego" -> campos
                    .put("cantidad", estado.cantidad.replace(',', '.'))
                    .put("unidad", estado.unidad)
                "cosecha" -> campos
                    .put("modalidad", estado.modalidad)
                    .put("kilos", estado.kilos.replace(',', '.'))
            }
            val cuerpo = JSONObject()
                .put("fecha", iso)
                .put("parcela_id", estado.parcelaId)
                .put("categoria", estado.categoria)
                .put("campos", campos)
                .put("observaciones", estado.observaciones)
            val editada = laborEditada
            if (editada != null) {
                laboresRepo.actualizarLabor(token, editada.id, cuerpo) { exito, texto ->
                    _uiState.update {
                        it.copy(trabajando = false, guardada = exito, mensaje = texto)
                    }
                }
            } else {
                laboresRepo.guardarLabor(token, cuerpo) { campana, texto ->
                    _uiState.update {
                        it.copy(
                            trabajando = false,
                            guardada = campana != null,
                            campanaGuardada = campana,
                            mensaje = texto
                        )
                    }
                }
            }
        }
    }
}

/** Un número guardado en los campos de la labor, para prellenar el formulario. */
private fun numeroInicial(labor: LaborEditable?, clave: String): String {
    val campos = labor?.campos ?: return ""
    if (!campos.has(clave)) return ""
    return formatearCantidad(campos.optDouble(clave))
}

/**
 * Formulario de una labor de "Mi cuaderno" (features alta-manual-labor y
 * edicion-de-labor): apunta una labor nueva o edita una existente.
 *
 * Campos por categoría según ADR 0005. La labor se guarda en la campaña
 * que corresponde a su fecha (campaña oleícola del 1 de noviembre al
 * 31 de octubre).
 */
@Composable
fun PantallaNuevaLabor(
    onVolver: () -> Unit,
    onVerCampana: (String) -> Unit,
    labor: LaborEditable? = null,
    viewModel: NuevaLaborViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.preparar(labor)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (estado.esEdicion) "Editar labor" else "Apuntar una labor",
            style = TextoTitulo1,
            color = Texto
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (estado.esEdicion) {
                "Los cambios se reflejan en Mi cuaderno."
            } else {
                "Va a Mi cuaderno, en la campaña de su fecha."
            },
            style = TextoCuerpo,
            color = Texto,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CampoDeTexto(
            etiqueta = "Fecha (día/mes/año)",
            valor = estado.fecha,
            alCambiar = viewModel::cambiarFecha,
            tipo = KeyboardType.Text
        )
        Spacer(modifier = Modifier.height(16.dp))
        Desplegable(
            etiqueta = "Parcela",
            valor = estado.parcelaNombre,
            opciones = estado.parcelas.map { it.nombre },
            alElegir = viewModel::elegirParcela
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¿Qué labor es?",
            style = TextoCuerpo,
            color = Texto
        )
        Spacer(modifier = Modifier.height(8.dp))
        OpcionCategoria(
            texto = "Tratamiento fitosanitario",
            elegida = estado.categoria == "tratamiento",
            alPulsar = { viewModel.elegirCategoria("tratamiento") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OpcionCategoria(
            texto = "Riego",
            elegida = estado.categoria == "riego",
            alPulsar = { viewModel.elegirCategoria("riego") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OpcionCategoria(
            texto = "Cosecha",
            elegida = estado.categoria == "cosecha",
            alPulsar = { viewModel.elegirCategoria("cosecha") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        when (estado.categoria) {
            "tratamiento" -> {
                CampoDeTexto(
                    etiqueta = "Producto",
                    valor = estado.producto,
                    alCambiar = viewModel::cambiarProducto,
                    tipo = KeyboardType.Text
                )
                Spacer(modifier = Modifier.height(16.dp))
                Desplegable(
                    etiqueta = "Plaga a controlar",
                    valor = estado.plaga,
                    opciones = listOf(
                        "mosca del olivo", "repilo", "prays", "tuberculosis",
                        "cochinilla", "malas hierbas", "otra"
                    ),
                    alElegir = viewModel::cambiarPlaga
                )
                Spacer(modifier = Modifier.height(16.dp))
                CampoDeTexto(
                    etiqueta = "Cantidad",
                    valor = estado.cantidad,
                    alCambiar = viewModel::cambiarCantidad,
                    tipo = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(16.dp))
                Desplegable(
                    etiqueta = "Unidad",
                    valor = estado.unidad,
                    opciones = listOf("kg", "L", "g", "ml"),
                    alElegir = viewModel::elegirUnidad
                )
            }
            "riego" -> {
                CampoDeTexto(
                    etiqueta = "Cantidad",
                    valor = estado.cantidad,
                    alCambiar = viewModel::cambiarCantidad,
                    tipo = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(16.dp))
                Desplegable(
                    etiqueta = "Unidad",
                    valor = estado.unidad,
                    opciones = listOf("horas", "m³", "L"),
                    alElegir = viewModel::elegirUnidad
                )
            }
            "cosecha" -> {
                Desplegable(
                    etiqueta = "Modalidad",
                    valor = estado.modalidad,
                    opciones = listOf("a vuelo", "a suelo"),
                    alElegir = viewModel::elegirModalidad
                )
                Spacer(modifier = Modifier.height(16.dp))
                CampoDeTexto(
                    etiqueta = "Kilos",
                    valor = estado.kilos,
                    alCambiar = viewModel::cambiarKilos,
                    tipo = KeyboardType.Decimal
                )
            }
        }
        if (estado.categoria.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            CampoDeTexto(
                etiqueta = "Observaciones (opcional)",
                valor = estado.observaciones,
                alCambiar = viewModel::cambiarObservaciones,
                tipo = KeyboardType.Text
            )
            Spacer(modifier = Modifier.height(24.dp))
            BotonPrincipal(
                texto = if (estado.trabajando) {
                    if (estado.esEdicion) "Guardando los cambios…" else "Guardando la labor…"
                } else {
                    if (estado.esEdicion) "Guardar cambios" else "Guardar labor"
                },
                habilitado = !estado.trabajando,
                alPulsar = viewModel::guardar
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        estado.mensaje?.let {
            Text(
                text = it,
                style = TextoCuerpo,
                color = if (estado.guardada) Primario else Error,
                textAlign = TextAlign.Center
            )
        }
        estado.campanaGuardada?.let {
            Spacer(modifier = Modifier.height(24.dp))
            BotonSecundario(
                texto = "Ver Mi cuaderno de esa campaña",
                alPulsar = { onVerCampana(it) }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        BotonSecundario(texto = "Volver a Mi cuaderno", alPulsar = onVolver)
    }
}
