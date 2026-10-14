package com.olivapp.app.acceso.presentation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.acceso.domain.CuentasRepositorio
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.FondoCrema
import com.olivapp.app.core.ui.TextoError
import com.olivapp.app.core.ui.TextoMarron
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Estado visible de la pantalla de inicio de sesión. */
data class InicioSesionUiState(
    val email: String = "",
    val contrasena: String = "",
    val trabajando: Boolean = false,
    val mensaje: String? = null,
    val sesionAbierta: Boolean = false
)

@HiltViewModel
class InicioSesionViewModel @Inject constructor(
    private val cuentas: CuentasRepositorio,
    private val sesion: SesionRepositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(InicioSesionUiState())
    val uiState: StateFlow<InicioSesionUiState> = _uiState.asStateFlow()

    fun cambiarEmail(valor: String) {
        _uiState.update { it.copy(email = valor) }
    }

    fun cambiarContrasena(valor: String) {
        _uiState.update { it.copy(contrasena = valor) }
    }

    fun iniciarSesion() {
        val estado = _uiState.value
        if (estado.trabajando) return
        _uiState.update { it.copy(trabajando = true, mensaje = null) }
        cuentas.iniciarSesion(estado.email, estado.contrasena) { token, texto ->
            if (token != null) {
                sesion.guardar(token)
                _uiState.update { it.copy(trabajando = false, sesionAbierta = true) }
            } else {
                _uiState.update { it.copy(trabajando = false, mensaje = texto) }
            }
        }
    }
}

@Composable
fun PantallaInicioSesion(
    onVolver: () -> Unit,
    onCrearCuenta: () -> Unit,
    onSesionAbierta: () -> Unit,
    visita: Int = 0,
    viewModel: InicioSesionViewModel = hiltViewModel(key = "visita-$visita")
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Al quedar la sesión abierta (token guardado), se entra al panel.
    LaunchedEffect(estado.sesionAbierta) {
        if (estado.sesionAbierta) onSesionAbierta()
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
            text = "Iniciar sesión",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Entra con tu email y contraseña.",
            fontSize = 18.sp,
            color = TextoMarron,
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
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(
            texto = if (estado.trabajando) "Entrando…" else "Iniciar sesión",
            habilitado = !estado.trabajando,
            alPulsar = viewModel::iniciarSesion
        )
        Spacer(modifier = Modifier.height(24.dp))
        estado.mensaje?.let {
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoError,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        BotonSecundario(texto = "Crear cuenta", alPulsar = onCrearCuenta)
        Spacer(modifier = Modifier.height(16.dp))
        BotonSecundario(texto = "Volver al inicio", alPulsar = onVolver)
    }
}
