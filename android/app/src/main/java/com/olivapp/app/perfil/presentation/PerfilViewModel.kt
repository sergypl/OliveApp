package com.olivapp.app.perfil.presentation

import androidx.lifecycle.ViewModel
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.perfil.domain.DatosPerfil
import com.olivapp.app.perfil.domain.PerfilRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Estado visible del perfil: cargando (sin perfil ni mensaje), contenido
 * (con perfil) o error (con mensaje).
 */
data class PerfilUiState(
    val perfil: DatosPerfil? = null,
    val mensaje: String? = null,
    val sinConexion: Boolean = false,
    val sesionCerrada: Boolean = false
)

private const val PREFIJO_SIN_CONEXION = "No hay conexión"

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val sesion: SesionRepositorio,
    private val perfilRepo: PerfilRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    /** Carga el perfil con el token guardado (a cada entrada y al reintentar). */
    fun cargar() {
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(
                    mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.",
                    sinConexion = false
                )
            }
            return
        }
        _uiState.update { it.copy(mensaje = null, sinConexion = false) }
        perfilRepo.cargarPerfil(token) { perfil, texto ->
            _uiState.update {
                if (perfil != null) {
                    it.copy(perfil = perfil, mensaje = null, sinConexion = false)
                } else {
                    it.copy(
                        mensaje = texto,
                        sinConexion = texto.startsWith(PREFIJO_SIN_CONEXION)
                    )
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
