package com.olivapp.app.acceso.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.acceso.domain.CuentasRepositorio
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.Fondo
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.PantallaConBarraSuperiorEnLista
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoEtiqueta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.olivapp.app.R

/** Estado visible de la pantalla de creación de cuenta. */
data class RegistroUiState(
    val email: String = "",
    val contrasena: String = "",
    val codigoSocio: String = "",
    val trabajando: Boolean = false,
    val mensaje: String? = null,
    val registrada: Boolean = false
)

@HiltViewModel
class RegistroViewModel @Inject constructor(
    private val cuentas: CuentasRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun cambiarEmail(valor: String) {
        _uiState.update { it.copy(email = valor) }
    }

    fun cambiarContrasena(valor: String) {
        _uiState.update { it.copy(contrasena = valor) }
    }

    fun cambiarCodigoSocio(valor: String) {
        _uiState.update { it.copy(codigoSocio = valor) }
    }

    fun crearCuenta() {
        val estado = _uiState.value
        if (estado.trabajando) return
        _uiState.update { it.copy(trabajando = true, mensaje = null, registrada = false) }
        cuentas.crearCuenta(estado.email, estado.contrasena, estado.codigoSocio) { exitosa, texto ->
            _uiState.update {
                it.copy(trabajando = false, registrada = exitosa, mensaje = texto)
            }
        }
    }
}

@Composable
fun PantallaRegistro(
    onVolver: () -> Unit,
    onIniciarSesion: () -> Unit,
    viewModel: RegistroViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    PantallaConBarraSuperiorEnLista(titulo = "Crear cuenta", onVolver = onVolver) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Con tu email, una contraseña y tu código de socio.",
                style = TextoCuerpo,
                color = Texto,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            CampoDeTexto(
                etiqueta = "Email",
                valor = estado.email,
                alCambiar = viewModel::cambiarEmail,
                tipo = KeyboardType.Email
            )
            Spacer(modifier = Modifier.height(16.dp))
            CampoDeTexto(
                etiqueta = "Contraseña",
                valor = estado.contrasena,
                alCambiar = viewModel::cambiarContrasena,
                tipo = KeyboardType.Password,
                oculto = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mínimo 8 caracteres.",
                style = TextoEtiqueta,
                color = Texto
            )
            Spacer(modifier = Modifier.height(16.dp))
            CampoDeTexto(
                etiqueta = "Código de socio",
                valor = estado.codigoSocio,
                alCambiar = viewModel::cambiarCodigoSocio,
                tipo = KeyboardType.Text
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "El que te ha dado tu cooperativa (p. ej. Socio 1).",
                style = TextoEtiqueta,
                color = Texto
            )
            Spacer(modifier = Modifier.height(24.dp))
            BotonPrincipal(
                texto = if (estado.trabajando) "Creando la cuenta…" else "Crear cuenta",
                icono = R.drawable.ic_crear_cuenta,
                habilitado = !estado.trabajando,
                alPulsar = viewModel::crearCuenta
            )
            Spacer(modifier = Modifier.height(24.dp))
            estado.mensaje?.let {
                Text(
                    text = it,
                    style = TextoCuerpo,
                    color = if (estado.registrada) Primario else Error,
                    textAlign = TextAlign.Center
                )
            }
            if (estado.registrada) {
                Spacer(modifier = Modifier.height(24.dp))
                BotonSecundario(texto = "Ir a iniciar sesión", icono = R.drawable.ic_iniciar_sesion, alPulsar = onIniciarSesion)
            }
            Spacer(modifier = Modifier.height(24.dp))
            BotonSecundario(texto = "Volver al inicio", icono = R.drawable.ic_volver, alPulsar = onVolver)
        }
    }
}
