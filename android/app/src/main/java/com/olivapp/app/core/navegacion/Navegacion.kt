package com.olivapp.app.core.navegacion

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.olivapp.app.AlmacenSesiones
import com.olivapp.app.LaborEditable
import com.olivapp.app.PantallaBienvenida
import com.olivapp.app.PantallaCampana
import com.olivapp.app.PantallaCuaderno
import com.olivapp.app.PantallaInicioSesion
import com.olivapp.app.PantallaNotasVoz
import com.olivapp.app.PantallaNuevaLabor
import com.olivapp.app.PantallaPanel
import com.olivapp.app.PantallaParcelas
import com.olivapp.app.PantallaRegistro
import com.olivapp.app.PantallaSimulador
import com.olivapp.app.SincronizadorNotas

/**
 * Navegación de la app: un router propio sobre los nombres de pantalla (sin
 * Navigation library). El flujo es idéntico al del refactor: cada pantalla
 * cambia con su estado y sus callbacks de salida.
 */
@Composable
fun OlivApp() {
    val contexto = LocalContext.current
    // Si ya hay token guardado, la sesión sigue abierta tras reiniciar la app.
    var pantalla by remember {
        mutableStateOf(if (AlmacenSesiones(contexto).hayToken()) "sesion" else "inicio")
    }
    var idCampana by remember { mutableStateOf<String?>(null) }
    var laborEnEdicion by remember { mutableStateOf<LaborEditable?>(null) }

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
            onVolver = { pantalla = "inicio" },
            onIniciarSesion = { pantalla = "inicio_sesion" }
        )
        "inicio_sesion" -> PantallaInicioSesion(
            onVolver = { pantalla = "inicio" },
            onCrearCuenta = { pantalla = "registro" },
            onSesionAbierta = { pantalla = "sesion" }
        )
        "campana" -> PantallaCampana(
            campanaId = idCampana ?: "",
            onVolver = { pantalla = "sesion" }
        )
        "sesion" -> PantallaPanel(
            onSesionCerrada = { pantalla = "inicio" },
            onAbrirParcelas = { pantalla = "parcelas" },
            onAbrirCuaderno = { pantalla = "cuaderno" },
            onAbrirNotasVoz = { pantalla = "notas_voz" },
            onAbrirSimulador = { pantalla = "simulador" },
            onAbrirCampana = { id ->
                idCampana = id
                pantalla = "campana"
            }
        )
        "parcelas" -> PantallaParcelas(onVolver = { pantalla = "sesion" })
        "notas_voz" -> PantallaNotasVoz(onVolver = { pantalla = "sesion" })
        "simulador" -> PantallaSimulador(onVolver = { pantalla = "sesion" })
        "cuaderno" -> PantallaCuaderno(
            onVolver = { pantalla = "sesion" },
            onApuntarLabor = {
                laborEnEdicion = null
                pantalla = "nueva_labor"
            },
            onEditarLabor = { labor ->
                laborEnEdicion = labor
                pantalla = "nueva_labor"
            }
        )
        "nueva_labor" -> PantallaNuevaLabor(
            onVolver = { pantalla = "cuaderno" },
            onVerCampana = { id ->
                idCampana = id
                pantalla = "campana"
            },
            labor = laborEnEdicion
        )
        else -> PantallaBienvenida(
            onIniciarSesion = { pantalla = "inicio_sesion" },
            onCrearCuenta = { pantalla = "registro" }
        )
    }
}
