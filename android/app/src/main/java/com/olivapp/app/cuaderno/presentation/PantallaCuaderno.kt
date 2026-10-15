package com.olivapp.app.cuaderno.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.R
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.core.formatos.NOMBRES_DE_CATEGORIA
import com.olivapp.app.core.ui.BotonMicrofonoFlotante
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.CabeceraSeccion
import com.olivapp.app.core.ui.ContenedorRefrescar
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.EsqueletoDeCarga
import com.olivapp.app.core.ui.EstadoVacio
import com.olivapp.app.core.ui.Fondo
import com.olivapp.app.core.ui.PantallaConBarraSuperiorGrandeEnLista
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoBoton
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.core.ui.mostrarConDeshacer
import com.olivapp.app.cuaderno.domain.DatosCampanaDeLabores
import com.olivapp.app.cuaderno.domain.DatosLabor
import com.olivapp.app.cuaderno.domain.FiltroDeCuaderno
import com.olivapp.app.cuaderno.domain.LaboresRepositorio
import com.olivapp.app.cuaderno.domain.LaborEditable
import com.olivapp.app.cuaderno.domain.agruparPorMes
import com.olivapp.app.cuaderno.domain.filtrarCampanas
import com.olivapp.app.cuaderno.domain.opcionesDeFiltro
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Estado visible de "Mi cuaderno". */
data class CuadernoUiState(
    val campanas: List<DatosCampanaDeLabores> = emptyList(),
    val cargado: Boolean = false,
    val refrescando: Boolean = false,
    val filtro: FiltroDeCuaderno = FiltroDeCuaderno(),
    val mensaje: String? = null,
    val laborParaEliminar: LaborEditable? = null,
    /** Labor ya quitada de la lista, esperando a que se cierre su mensaje de "Deshacer". */
    val laborBorrada: LaborEditable? = null
)

/** Una labor quitada de la lista, con su sitio para poder volver a ponerla. */
private class BorradoPendiente(
    val campana: String,
    val indice: Int,
    val labor: DatosLabor
)

@HiltViewModel
class CuadernoViewModel @Inject constructor(
    private val sesion: SesionRepositorio,
    private val laboresRepo: LaboresRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(CuadernoUiState())
    val uiState: StateFlow<CuadernoUiState> = _uiState.asStateFlow()

    private var borradoPendiente: BorradoPendiente? = null

    /** Carga el cuaderno con el token guardado (a cada entrada en la pantalla). */
    fun cargar() {
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(
                    mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.",
                    cargado = true,
                    refrescando = false
                )
            }
            return
        }
        laboresRepo.cargarMisLabores(token) { lista, texto ->
            _uiState.update {
                it.copy(
                    cargado = true,
                    refrescando = false,
                    // La labor con su mensaje de "Deshacer" abierto sigue fuera
                    // de la lista hasta que se deshaga o se borre de verdad.
                    campanas = lista?.let { cargadas -> sinBorradoPendiente(cargadas) }
                        ?: it.campanas,
                    mensaje = if (lista != null) it.mensaje else texto
                )
            }
        }
    }

    /** Deslizar hacia abajo para refrescar. */
    fun alRefrescar() {
        _uiState.update { it.copy(refrescando = true, mensaje = null) }
        cargar()
    }

    /** Chip de categoría de la línea de tiempo (null = todas). */
    fun filtrarPorCategoria(categoria: String?) {
        _uiState.update { it.copy(filtro = it.filtro.copy(categoria = categoria)) }
    }

    /** Chip de parcela de la línea de tiempo (null = todas). */
    fun filtrarPorParcela(parcela: String?) {
        _uiState.update { it.copy(filtro = it.filtro.copy(parcela = parcela)) }
    }

    /** El socio pide eliminar una labor: se le pide confirmación antes. */
    fun pedirEliminar(labor: LaborEditable) {
        _uiState.update { it.copy(laborParaEliminar = labor) }
    }

    fun cancelarEliminar() {
        _uiState.update { it.copy(laborParaEliminar = null) }
    }

    /**
     * Borrado con "Deshacer": la labor desaparece de la lista al momento, pero
     * solo se borra en el backend cuando se cierre el mensaje sin deshacer
     * (ver [cerrarAvisoDeBorrado]). No hay papelera que la recupere después.
     */
    fun confirmarEliminar() {
        val objetivo = _uiState.value.laborParaEliminar ?: return
        // Si ya había un borrado esperando su mensaje, se cierra: se borra ya.
        cerrarAvisoDeBorrado()
        val estado = _uiState.value
        val campana = estado.campanas.firstOrNull { c ->
            c.labores.any { it.editable.id == objetivo.id }
        } ?: return
        val indice = campana.labores.indexOfFirst { it.editable.id == objetivo.id }
        borradoPendiente = BorradoPendiente(campana.nombre, indice, campana.labores[indice])
        _uiState.update {
            it.copy(
                laborParaEliminar = null,
                laborBorrada = objetivo,
                campanas = sinBorradoPendiente(it.campanas)
            )
        }
    }

    /** El socio pulsó "Deshacer": la labor vuelve a su sitio y no se borra nada. */
    fun deshacerEliminar() {
        val pendiente = borradoPendiente ?: return
        borradoPendiente = null
        _uiState.update {
            it.copy(
                campanas = reponerLabor(it.campanas, pendiente),
                laborBorrada = null,
                mensaje = null
            )
        }
    }

    /**
     * Se cerró el mensaje sin deshacer (o se sale de la pantalla con él
     * abierto): ahora sí se borra la labor en el backend. Si el borrado falla,
     * la labor vuelve a la lista y se avisa.
     */
    fun cerrarAvisoDeBorrado() {
        val pendiente = borradoPendiente ?: return
        borradoPendiente = null
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(
                    campanas = reponerLabor(it.campanas, pendiente),
                    laborBorrada = null,
                    mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
                )
            }
            return
        }
        laboresRepo.eliminarLabor(token, pendiente.labor.editable.id) { exito, _ ->
            if (exito) {
                // El aviso con "Deshacer" ya dijo lo que pasaba: solo queda
                // recargar para que la lista cuadre con el backend.
                _uiState.update { it.copy(laborBorrada = null) }
                cargar()
            } else {
                _uiState.update {
                    it.copy(
                        campanas = reponerLabor(it.campanas, pendiente),
                        laborBorrada = null,
                        mensaje = "No se pudo eliminar la labor. Vuelve a estar en tu cuaderno."
                    )
                }
            }
        }
    }

    /** Al salir de la pantalla con el mensaje abierto, la labor se borra. */
    fun alSalir() {
        cerrarAvisoDeBorrado()
    }

    private fun sinBorradoPendiente(campanas: List<DatosCampanaDeLabores>):
        List<DatosCampanaDeLabores> {
        val idPendiente = borradoPendiente?.labor?.editable?.id ?: return campanas
        return campanas.map { campana ->
            DatosCampanaDeLabores(
                nombre = campana.nombre,
                esActual = campana.esActual,
                labores = campana.labores.filterNot { it.editable.id == idPendiente }
            )
        }
    }

    private fun reponerLabor(
        campanas: List<DatosCampanaDeLabores>,
        pendiente: BorradoPendiente
    ): List<DatosCampanaDeLabores> {
        if (campanas.none { it.nombre == pendiente.campana }) {
            return listOf(
                DatosCampanaDeLabores(pendiente.campana, false, listOf(pendiente.labor))
            ) + campanas
        }
        return campanas.map { campana ->
            if (campana.nombre != pendiente.campana) {
                campana
            } else {
                val labores = campana.labores.toMutableList()
                labores.add(pendiente.indice.coerceIn(0, labores.size), pendiente.labor)
                DatosCampanaDeLabores(campana.nombre, campana.esActual, labores)
            }
        }
    }
}

/**
 * Pantalla "Mi cuaderno" (features consulta-cuaderno-por-campana y
 * cuaderno-linea-de-tiempo).
 *
 * Historial cronológico de las labores del socio como línea de tiempo: cabecera
 * de campaña, cabeceras de mes fijas y, dentro de cada mes, las labores de la
 * más reciente a la más antigua con el icono y el color de su categoría. Se
 * puede filtrar por categoría y parcela, deslizar hacia abajo para refrescar y
 * eliminar con "Deshacer". Solo labores vigentes (sin papelera).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PantallaCuaderno(
    onApuntarLabor: () -> Unit,
    onEditarLabor: (LaborEditable) -> Unit,
    // Acción del botón flotante "Dictar nota": la pantalla no sabe adónde va.
    onDictarNota: () -> Unit = {},
    viewModel: CuadernoViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    // Al salir de la pantalla con el mensaje de "Deshacer" abierto, la labor
    // se borra igualmente: no queda ningún borrado a medias.
    DisposableEffect(Unit) {
        onDispose { viewModel.alSalir() }
    }

    // El aviso con "Deshacer": si se cierra sin deshacer, la labor se borra.
    estado.laborBorrada?.let { borrada ->
        LaunchedEffect(borrada.id) {
            val deshecho = avisos.mostrarConDeshacer("Labor eliminada de tu cuaderno.")
            if (deshecho) {
                viewModel.deshacerEliminar()
            } else {
                viewModel.cerrarAvisoDeBorrado()
            }
        }
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

    val opciones = remember(estado.campanas) { opcionesDeFiltro(estado.campanas) }
    val grupos = remember(estado.campanas, estado.filtro) {
        agruparPorMes(filtrarCampanas(estado.campanas, estado.filtro))
    }

    PantallaConBarraSuperiorGrandeEnLista(
        titulo = "Mi cuaderno",
        aviso = avisos,
        accionFlotante = {
            BotonMicrofonoFlotante(alPulsar = onDictarNota)
        }
    ) {
        ContenedorRefrescar(
            refrescando = estado.refrescando,
            alRefrescar = viewModel::alRefrescar,
            modificador = Modifier.fillMaxSize()
        ) {
            when {
                !estado.cargado -> EsqueletoDeCarga(
                    filas = 6,
                    altoFila = 72.dp,
                    modificador = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
                estado.campanas.all { it.labores.isEmpty() } -> EstadoVacio(
                    titulo = "Apunta tu primera labor",
                    descripcion = "Tus labores aparecerán aquí por orden de fecha.",
                    textoAccion = "Apuntar una labor",
                    alPulsarAccion = onApuntarLabor,
                    modificador = Modifier.padding(top = 32.dp)
                )
                grupos.isEmpty() -> EstadoVacio(
                    titulo = "No hay labores con estos filtros",
                    descripcion = "Prueba con otra categoría o quita los filtros.",
                    textoAccion = "Quitar los filtros",
                    alPulsarAccion = {
                        viewModel.filtrarPorCategoria(null)
                        viewModel.filtrarPorParcela(null)
                    },
                    modificador = Modifier.padding(top = 32.dp)
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item(key = "entrada") {
                        Text(
                            text = "Historial de tus labores por campaña,\nde la más reciente a la más antigua.",
                            style = TextoCuerpo,
                            color = Texto,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BotonPrincipal(
                            texto = "Apuntar una labor",
                            icono = R.drawable.ic_anadir,
                            alPulsar = onApuntarLabor
                        )
                    }
                    item(key = "filtros") {
                        Spacer(modifier = Modifier.height(24.dp))
                        FiltrosDeCuaderno(
                            opciones = opciones,
                            filtro = estado.filtro,
                            alFiltrarCategoria = viewModel::filtrarPorCategoria,
                            alFiltrarParcela = viewModel::filtrarPorParcela
                        )
                    }
                    for (campana in grupos) {
                        item(key = "campana-" + campana.nombre) {
                            Spacer(modifier = Modifier.height(24.dp))
                            CabeceraSeccion(
                                titulo = "Campaña " + campana.nombre +
                                    if (campana.esActual) " · Campaña actual" else "",
                                icono = R.drawable.ic_campana
                            )
                        }
                        for (mes in campana.meses) {
                            stickyHeader(key = "mes-" + mes.clave) {
                                // Cabecera fija: fondo opaco para que la lista
                                // pase por debajo sin transparentarse.
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Fondo)
                                        .padding(vertical = 12.dp)
                                ) {
                                    CabeceraDeMes(grupo = mes)
                                }
                            }
                            items(
                                items = mes.labores,
                                key = { "labor-" + it.editable.id }
                            ) { labor ->
                                TarjetaDeLabor(
                                    labor = labor,
                                    alEditar = { onEditarLabor(labor.editable) },
                                    alEliminar = { viewModel.pedirEliminar(labor.editable) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                    item(key = "mensaje") {
                        estado.mensaje?.let {
                            Text(
                                text = it,
                                style = TextoCuerpo,
                                color = Error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
