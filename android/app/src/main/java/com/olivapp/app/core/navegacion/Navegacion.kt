package com.olivapp.app.core.navegacion

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.olivapp.app.acceso.data.AlmacenSesiones
import com.olivapp.app.acceso.presentation.PantallaBienvenida
import com.olivapp.app.acceso.presentation.PantallaInicioSesion
import com.olivapp.app.acceso.presentation.PantallaRegistro
import com.olivapp.app.LaborEditable
import com.olivapp.app.PantallaCuaderno
import com.olivapp.app.PantallaNotasVoz
import com.olivapp.app.PantallaNuevaLabor
import com.olivapp.app.PantallaSimulador
import com.olivapp.app.SincronizadorNotas
import com.olivapp.app.campana.presentation.PantallaCampana
import com.olivapp.app.campana.presentation.PantallaPanel
import com.olivapp.app.parcelas.presentation.PantallaParcelas

/**
 * Navegación de la app: un router propio sobre los nombres de pantalla (sin
 * Navigation library). El flujo es idéntico al del refactor: cada pantalla
 * cambia con su estado y sus callbacks de salida.
 */
@Composable
fun OlivApp() {
    val contexto = LocalContext.current
    // Si ya hay token guardado, la sesión sigue abierta tras reiniciar la app.
    // La navegación y su clave de visita sobreviven a la rotación (decisión del
    // refactor-arquitectura-android).
    var pantalla by rememberSaveable {
        mutableStateOf(if (AlmacenSesiones(contexto).hayToken()) "sesion" else "inicio")
    }
    var idCampana by rememberSaveable { mutableStateOf<String?>(null) }
    var laborEnEdicion by remember { mutableStateOf<LaborEditable?>(null) }
    // Clave única por entrada en una pantalla: cada visita tiene su ViewModel y
    // la rotación conserva el mismo (se pasa como `visita` a cada pantalla).
    var visita by rememberSaveable { mutableStateOf(0) }

    fun irA(destino: String) {
        pantalla = destino
        visita += 1
    }

    // Al recuperar la conexión, la cola de notas pendientes se envía sola a
    // transcribir (feature sincronizacion-pendientes-al-reconectar).
    DisposableEffect(Unit) {
        val gestor =
            contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(red: Network) {
                SincronizadorNotas.sincronizar(contexto)
            }
        }
        gestor.registerDefaultNetworkCallback(callback)
        onDispose { gestor.unregisterNetworkCallback(callback) }
    }

    when (pantalla) {
        "registro" -> PantallaRegistro(
            onVolver = { irA("inicio") },
            onIniciarSesion = { irA("inicio_sesion") },
            visita = visita
        )
        "inicio_sesion" -> PantallaInicioSesion(
            onVolver = { irA("inicio") },
            onCrearCuenta = { irA("registro") },
            onSesionAbierta = { irA("sesion") },
            visita = visita
        )
        "campana" -> PantallaCampana(
            campanaId = idCampana ?: "",
            onVolver = { irA("sesion") },
            visita = visita
        )
        "sesion" -> PantallaPanel(
            onSesionCerrada = { irA("inicio") },
            onAbrirParcelas = { irA("parcelas") },
            onAbrirCuaderno = { irA("cuaderno") },
            onAbrirNotasVoz = { irA("notas_voz") },
            onAbrirSimulador = { irA("simulador") },
            onAbrirCampana = { id ->
                idCampana = id
                irA("campana")
            },
            visita = visita
        )
        "parcelas" -> PantallaParcelas(onVolver = { irA("sesion") }, visita = visita)
        "notas_voz" -> PantallaNotasVoz(onVolver = { irA("sesion") })
        "simulador" -> PantallaSimulador(onVolver = { irA("sesion") })
        "cuaderno" -> PantallaCuaderno(
            onVolver = { irA("sesion") },
            onApuntarLabor = {
                laborEnEdicion = null
                irA("nueva_labor")
            },
            onEditarLabor = { labor ->
                laborEnEdicion = labor
                irA("nueva_labor")
            }
        )
        "nueva_labor" -> PantallaNuevaLabor(
            onVolver = { irA("cuaderno") },
            onVerCampana = { id ->
                idCampana = id
                irA("campana")
            },
            labor = laborEnEdicion
        )
        else -> PantallaBienvenida(
            onIniciarSesion = { irA("inicio_sesion") },
            onCrearCuenta = { irA("registro") }
        )
    }
}
