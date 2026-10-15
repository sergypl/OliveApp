package com.olivapp.app.perfil.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.BuildConfig
import com.olivapp.app.R
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CabeceraSeccion
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.EsqueletoDeCarga
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.FilaDatoApilada
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.SobrePrimario
import com.olivapp.app.core.ui.Tarjeta
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoSecundario
import com.olivapp.app.core.ui.TextoTitulo1
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.core.ui.pantallaCompleta
import com.olivapp.app.perfil.domain.DatosPerfil
import com.olivapp.app.perfil.domain.inicialesDe

/**
 * Pestaña Perfil (solo lectura): avatar de iniciales, datos del socio, aviso
 * de datos de demostración (ADR 0006), versión de la app y "Cerrar sesión".
 * No se edita nada aquí: ni datos, ni contraseña, ni foto.
 */
@Composable
fun PantallaPerfil(
    onSesionCerrada: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

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
        Text(
            text = "Tu perfil",
            style = TextoTitulo1,
            color = Texto,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        val perfil = estado.perfil
        when {
            perfil != null -> ContenidoPerfil(perfil)
            estado.mensaje != null -> ErrorDePerfil(
                mensaje = estado.mensaje ?: "",
                sinConexion = estado.sinConexion,
                alReintentar = viewModel::cargar
            )
            else -> {
                Text(
                    text = "Cargando tu perfil…",
                    style = TextoCuerpo,
                    color = TextoSecundario,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                EsqueletoDeCarga(filas = 5)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        AvisoDeDemostracion()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "OlivApp, versión ${BuildConfig.VERSION_NAME}",
            style = TextoCuerpo,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        BotonSecundario(
            texto = "Cerrar sesión",
            icono = R.drawable.ic_cerrar_sesion,
            alPulsar = viewModel::cerrarSesion
        )
    }
}

@Composable
private fun ContenidoPerfil(perfil: DatosPerfil) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(iniciales = inicialesDe(perfil.email))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = perfil.email,
            style = TextoValor,
            color = Texto,
            textAlign = TextAlign.Center
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    CabeceraSeccion(icono = R.drawable.ic_cooperativa, titulo = "Tus datos de socio")
    Tarjeta {
        FilaDato(etiqueta = "Código de socio", valor = perfil.codigoSocio, icono = R.drawable.ic_perfil)
        Spacer(modifier = Modifier.height(12.dp))
        FilaDatoApilada(etiqueta = "Cooperativa", valor = perfil.cooperativa, icono = R.drawable.ic_cooperativa)
        Spacer(modifier = Modifier.height(12.dp))
        FilaDato(
            etiqueta = "Parcelas",
            valor = perfil.numeroParcelas.toString(),
            icono = R.drawable.ic_parcelas
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilaDato(
            etiqueta = "Campaña actual",
            valor = perfil.campanaActual.ifEmpty { "Sin campaña" },
            icono = R.drawable.ic_campana
        )
    }
}

/** Círculo verde oliva con las iniciales; el email que lo acompaña ya dice quién es. */
@Composable
private fun Avatar(iniciales: String) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Primario)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(text = iniciales, style = TextoTitulo1, color = SobrePrimario)
    }
}

@Composable
private fun ErrorDePerfil(mensaje: String, sinConexion: Boolean, alReintentar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (sinConexion) R.drawable.ic_sin_conexion else R.drawable.ic_error
            ),
            contentDescription = null,
            tint = Error,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = if (sinConexion) "Sin conexión" else "No se pudo cargar tu perfil",
            style = TextoValor,
            color = Texto,
            textAlign = TextAlign.Center
        )
        Text(
            text = mensaje,
            style = TextoCuerpo,
            color = Error,
            textAlign = TextAlign.Center
        )
        BotonSecundario(texto = "Reintentar", icono = R.drawable.ic_refrescar, alPulsar = alReintentar)
    }
}

/** Aviso de datos de demostración (ADR 0006): los datos de la cooperativa no son reales. */
@Composable
private fun AvisoDeDemostracion() {
    Tarjeta {
        Text(
            text = "Datos de demostración",
            style = TextoValor,
            color = Texto
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Los datos de la cooperativa que ves en OlivApp son de demostración. " +
                "No representan a ninguna cooperativa real.",
            style = TextoCuerpo,
            color = TextoSecundario
        )
    }
}
