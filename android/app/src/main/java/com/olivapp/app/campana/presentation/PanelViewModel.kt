package com.olivapp.app.campana.presentation

import androidx.lifecycle.ViewModel
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.campana.domain.CampanasRepositorio
import com.olivapp.app.campana.domain.ComparativaAceite
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.DatosPanel
import com.olivapp.app.campana.domain.barrasDeAceite
import com.olivapp.app.campana.domain.BarraDeAceite
import com.olivapp.app.campana.domain.calcularComparativa
import com.olivapp.app.perfil.domain.DatosPerfil
import com.olivapp.app.perfil.domain.PerfilRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val PREFIJO_SIN_CONEXION = "No hay conexión"

/**
 * Estado visible del panel de la campaña actual.
 *
 * - [mensaje]: fallo al cargar los datos oficiales (nunca se confunde con "no
 *   hay datos": si no hay datos que enseñar se ofrece Reintentar).
 * - [errorDeCampanas]: fallo al cargar las campañas (comparativa y anteriores).
 * - [cargando]: primera carga en curso, sin datos aún (esqueleto).
 * - [refrescando]: carga lanzada deslizando hacia abajo.
 */
data class PanelUiState(
    val datos: DatosPanel? = null,
    val campanas: List<DatosCampanaResumen> = emptyList(),
    val perfil: DatosPerfil? = null,
    val mensaje: String? = null,
    val errorDeCampanas: String? = null,
    val cargando: Boolean = true,
    val refrescando: Boolean = false
) {
    val sinConexion: Boolean get() = mensaje?.startsWith(PREFIJO_SIN_CONEXION) == true

    val comparativa: ComparativaAceite? get() = calcularComparativa(campanas)

    val barras: List<BarraDeAceite> get() = barrasDeAceite(campanas)

    val campanasAnteriores: List<DatosCampanaResumen>
        get() = campanas.filter { it.estado == "cerrada" }
}

@HiltViewModel
class PanelViewModel @Inject constructor(
    private val sesion: SesionRepositorio,
    private val campanasRepo: CampanasRepositorio,
    private val perfilRepo: PerfilRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(PanelUiState())
    val uiState: StateFlow<PanelUiState> = _uiState.asStateFlow()

    /** Peticiones de la carga en curso (datos, campañas y perfil). */
    private var pendientes = 0

    /**
     * Carga los datos oficiales, las campañas y el perfil con el token
     * guardado. Se llama a cada entrada en la pantalla y al reintentar. Si ya
     * hay una carga en curso no lanza otra.
     */
    fun cargar() {
        if (pendientes > 0) return
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(
                    mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.",
                    cargando = false,
                    refrescando = false
                )
            }
            return
        }
        pendientes = 3
        // Con datos ya en pantalla la recarga es silenciosa: no vuelve el esqueleto.
        _uiState.update { it.copy(cargando = it.datos == null) }
        campanasRepo.cargarMiPanel(token) { cargados, texto ->
            try {
                _uiState.update {
                    if (cargados != null) {
                        it.copy(datos = cargados, mensaje = null)
                    } else {
                        it.copy(mensaje = texto.ifEmpty { "No se pudieron cargar tus datos." })
                    }
                }
            } finally {
                // Cómo termine la respuesta, la petición se descuenta: el panel
                // nunca se queda en "Cargando…" por una respuesta ilegible.
                terminarPeticion()
            }
        }
        campanasRepo.cargarCampanas(token) { lista, texto ->
            try {
                _uiState.update {
                    if (lista != null) {
                        it.copy(campanas = lista, errorDeCampanas = null)
                    } else {
                        it.copy(errorDeCampanas = texto)
                    }
                }
            } finally {
                terminarPeticion()
            }
        }
        // La cabecera (saludo, cooperativa y avatar) se degrada sin el perfil.
        perfilRepo.cargarPerfil(token) { perfil, _ ->
            try {
                if (perfil != null) _uiState.update { it.copy(perfil = perfil) }
            } finally {
                terminarPeticion()
            }
        }
    }

    /** Deslizar hacia abajo: recarga todo enseñando el indicador de refresco. */
    fun alRefrescar() {
        if (pendientes > 0) return
        _uiState.update { it.copy(refrescando = true) }
        cargar()
    }

    private fun terminarPeticion() {
        // Nunca por debajo de cero: un descuento de más dejaría el contador
        // cojo y el panel sin terminar de cargar jamás.
        pendientes = (pendientes - 1).coerceAtLeast(0)
        if (pendientes == 0) {
            _uiState.update { it.copy(cargando = false, refrescando = false) }
        }
    }
}
