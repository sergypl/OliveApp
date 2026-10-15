package com.olivapp.app.core.navegacion

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.olivapp.app.acceso.data.AlmacenSesiones
import com.olivapp.app.acceso.presentation.PantallaBienvenida
import com.olivapp.app.acceso.presentation.PantallaInicioSesion
import com.olivapp.app.acceso.presentation.PantallaRegistro
import com.olivapp.app.voz.presentation.PantallaNotasVoz
import com.olivapp.app.simulador.presentation.PantallaSimulador
import com.olivapp.app.voz.data.SincronizadorNotas
import com.olivapp.app.campana.presentation.PantallaCampana
import com.olivapp.app.campana.presentation.PantallaPanel
import com.olivapp.app.cuaderno.domain.LaborEditable
import com.olivapp.app.cuaderno.presentation.PantallaCuaderno
import com.olivapp.app.cuaderno.presentation.PantallaNuevaLabor
import com.olivapp.app.core.ui.LocalBarraInferiorVisible
import com.olivapp.app.parcelas.presentation.PantallaParcelas
import com.olivapp.app.perfil.presentation.PantallaPerfil

/**
 * Navegación de la app: un router propio sobre los nombres de pantalla (sin
 * Navigation library). Cada pantalla cambia con su estado y sus callbacks de
 * salida. Las cinco pestañas de primer nivel (Pestanas.kt) llevan la barra de
 * navegación inferior; las pantallas de segundo nivel y las de acceso no.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OlivApp() {
    val contexto = LocalContext.current
    val visitas: VisitasViewModel = hiltViewModel()
    // Si ya hay token guardado, la sesión sigue abierta tras reiniciar la app.
    // La navegación y su clave de visita sobreviven a la rotación (decisión del
    // refactor-arquitectura-android).
    var pantalla by rememberSaveable {
        mutableStateOf(if (AlmacenSesiones(contexto).hayToken()) "sesion" else "inicio")
    }
    var idCampana by rememberSaveable { mutableStateOf<String?>(null) }
    var laborEnEdicion by remember { mutableStateOf<LaborEditable?>(null) }
    // Clave única por entrada en una pantalla: cada visita tiene su propia
    // tienda de ViewModels (Ver VisitasViewModel) y la rotación conserva la de
    // la visita actual.
    var visita by rememberSaveable { mutableStateOf(0) }

    fun irA(destino: String) {
        // La visita que se abandona cierra su tienda: sus ViewModels reciben
        // onCleared y la próxima entrada empieza con estado limpio.
        visitas.cerrar(visita)
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

    // Botón atrás del sistema (lógica pura en Pestanas.kt): desde una pestaña
    // distinta de Inicio vuelve a Inicio; desde Inicio se deja pasar al
    // sistema, que cierra la app. Sin historial acumulado entre pestañas.
    val atras = accionDeAtras(pantalla)
    BackHandler(enabled = atras is AccionAtras.IrA) {
        if (atras is AccionAtras.IrA) irA(atras.destino)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Transición suave entre pantallas del router propio. La tienda de
        // ViewModels por visita se mantiene: cada contenido de la transición usa
        // el propietario de SU visita, así la pantalla saliente conserva sus
        // ViewModels mientras se desvanece y la entrante empieza con los suyos.
        AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            targetState = pantalla to visita,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) +
                    slideInVertically(initialOffsetY = { it / 12 })) togetherWith
                    (fadeOut(animationSpec = tween(180)) +
                        slideOutVertically(targetOffsetY = { -it / 12 }))
            },
            label = "pantalla"
        ) { (pantallaActual, visitaActual) ->
            val propietarioDeVisita = remember(visitaActual) {
                PropietarioDeVisita(
                    visitas.tiendaDe(visitaActual),
                    contexto as ComponentActivity
                )
            }
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides propietarioDeVisita,
                LocalBarraInferiorVisible provides barraInferiorVisible(pantallaActual)
            ) {
                when (pantallaActual) {
                    "registro" -> PantallaRegistro(
                        onVolver = { irA("inicio") },
                        onIniciarSesion = { irA("inicio_sesion") }
                    )
                    "inicio_sesion" -> PantallaInicioSesion(
                        onVolver = { irA("inicio") },
                        onCrearCuenta = { irA("registro") },
                        onSesionAbierta = { irA("sesion") }
                    )
                    "campana" -> PantallaCampana(
                        campanaId = idCampana ?: "",
                        onVolver = { irA("sesion") }
                    )
                    "sesion" -> PantallaPanel(
                        onAbrirPerfil = { irA("perfil") },
                        onAbrirNotasVoz = { irA("notas_voz") },
                        onAbrirCampana = { id ->
                            idCampana = id
                            irA("campana")
                        }
                    )
                    "parcelas" -> PantallaParcelas()
                    "perfil" -> PantallaPerfil(onSesionCerrada = { irA("inicio") })
                    "notas_voz" -> PantallaNotasVoz(onVolver = { irA("sesion") })
                    "simulador" -> PantallaSimulador()
                    "cuaderno" -> PantallaCuaderno(
                        onApuntarLabor = {
                            laborEnEdicion = null
                            irA("nueva_labor")
                        },
                        onEditarLabor = { labor ->
                            laborEnEdicion = labor
                            irA("nueva_labor")
                        },
                        onDictarNota = { irA("notas_voz") }
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
        }
        // Barra fija sobre el contenido (las pantallas reservan su hueco con
        // pantallaCompleta). Se retira con el teclado abierto para no robar
        // espacio al formulario.
        if (barraInferiorVisible(pantalla) && !WindowInsets.isImeVisible) {
            BarraInferior(
                activa = pestanaDe(pantalla),
                alElegir = { pestana -> pantallaAlElegir(pestana, pantalla)?.let(::irA) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
