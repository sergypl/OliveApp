package com.olivapp.app.voz.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.FondoCrema
import com.olivapp.app.core.ui.TextoError
import com.olivapp.app.core.ui.TextoMarron
import com.olivapp.app.core.ui.VerdeOliva
import com.olivapp.app.voz.data.AlmacenNotasVoz
import com.olivapp.app.voz.data.SincronizadorNotas
import com.olivapp.app.voz.domain.GrabadorDeNotas
import com.olivapp.app.voz.domain.NotasRepositorio
import com.olivapp.app.voz.domain.aplicarTranscripcion
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Límite de duración de una nota de voz (decisión de esta feature). */
private const val LIMITE_SEGUNDOS_NOTA = 60

/** Estado visible de la pantalla de notas de voz: la cola local y la grabación. */
data class NotasVozUiState(
    val notas: List<AlmacenNotasVoz.NotaDeVoz> = emptyList(),
    val grabando: Boolean = false,
    val segundos: Int = 0,
    val mensaje: String? = null
)

@HiltViewModel
class NotasVozViewModel @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val sesion: SesionRepositorio,
    private val notasRepo: NotasRepositorio,
    private val grabador: GrabadorDeNotas
) : ViewModel() {

    private var ficheroActual: File? = null
    private var reloj: Job? = null

    private val _uiState = MutableStateFlow(NotasVozUiState(notas = notasRepo.notas()))
    val uiState: StateFlow<NotasVozUiState> = _uiState.asStateFlow()

    init {
        // La cola también cambia sola al recuperar la conexión: el
        // SincronizadorNotas avisa y se vuelve a leer.
        SincronizadorNotas.alCambiar = { refrescar() }
    }

    /** Al abrir la pantalla se sincroniza la cola: las notas pendientes se
     * envían en orden de grabación y sin duplicados. */
    fun abrir() {
        SincronizadorNotas.sincronizar(contexto)
    }

    /** Al salir de la pantalla se desengancha el aviso del sincronizador y, si
     * quedó una grabación a medias, su audio no se queda en el teléfono. */
    fun alSalir() {
        SincronizadorNotas.alCambiar = null
        reloj?.cancel()
        if (_uiState.value.grabando) {
            grabador.parar()
            ficheroActual?.delete()
            ficheroActual = null
            _uiState.update { it.copy(grabando = false) }
        }
    }

    override fun onCleared() {
        alSalir()
    }

    fun empezarGrabacion() {
        try {
            val fichero = notasRepo.nuevaRutaDeAudio()
            grabador.empezar(fichero)
            ficheroActual = fichero
            _uiState.update { it.copy(segundos = 0, grabando = true, mensaje = null) }
            empezarReloj(LIMITE_SEGUNDOS_NOTA) {
                pararGrabacion()
                _uiState.update {
                    it.copy(
                        mensaje = "Has llegado a los $LIMITE_SEGUNDOS_NOTA segundos. " +
                            "Nota guardada y en transcripción."
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(mensaje = "No se pudo empezar a grabar. Comprueba el permiso de micrófono.")
            }
        }
    }

    fun pararGrabacion() {
        val duracion = grabador.parar()
        reloj?.cancel()
        val fichero = ficheroActual
        ficheroActual = null
        _uiState.update { it.copy(grabando = false) }
        if (duracion >= 1 && fichero != null) {
            val nota = notasRepo.guardar(fichero, duracion)
            refrescar()
            _uiState.update { it.copy(mensaje = "Nota de voz guardada. Transcribiendo…") }
            transcribir(nota)
        } else {
            fichero?.delete()
            _uiState.update {
                it.copy(mensaje = "La grabación ha sido demasiado corta. Vuelve a intentarlo.")
            }
        }
    }

    /** Sin permiso de micrófono no se puede grabar: se dice claro. */
    fun sinPermiso() {
        _uiState.update {
            it.copy(mensaje = "Sin permiso de micrófono no se pueden grabar notas de voz.")
        }
    }

    /** Envía el audio de una nota a transcribir (también al reintentar). */
    fun transcribir(nota: AlmacenNotasVoz.NotaDeVoz) {
        val token = sesion.token()
        if (token == null) {
            notasRepo.marcar(nota.id, "error", "")
            refrescar()
            _uiState.update {
                it.copy(mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión.")
            }
        } else {
            notasRepo.marcar(nota.id, "transcribiendo", "")
            refrescar()
            notasRepo.transcribirNota(
                token,
                File(nota.fichero)
            ) { texto, borrador, error, sinConexion, audioInvalido ->
                val mensaje = aplicarTranscripcion(
                    notasRepo,
                    nota.id,
                    texto,
                    borrador,
                    error,
                    sinConexion,
                    audioInvalido
                )
                refrescar()
                _uiState.update { it.copy(mensaje = mensaje) }
            }
        }
    }

    /** El borrador IA de una nota ha cambiado: se guarda en su ficha. */
    fun guardarBorrador(notaId: String, borrador: String) {
        notasRepo.actualizarBorrador(notaId, borrador)
        refrescar()
    }

    /** La nota queda confirmada: su borrador ya se ha convertido en labor de
     * Mi cuaderno. */
    fun confirmarBorrador(nota: AlmacenNotasVoz.NotaDeVoz) {
        notasRepo.marcar(nota.id, "confirmada", nota.texto, nota.borrador)
        refrescar()
    }

    /** La nota queda descartada: el socio rechaza el borrador y no se crea
     * ninguna labor. */
    fun descartarBorrador(nota: AlmacenNotasVoz.NotaDeVoz) {
        notasRepo.marcar(nota.id, "descartada", nota.texto, nota.borrador)
        refrescar()
    }

    private fun refrescar() {
        _uiState.update { it.copy(notas = notasRepo.notas()) }
    }

    /** Reloj de la grabación: se detiene sola al llegar al límite. */
    private fun empezarReloj(limite: Int, alLlegarAlLimite: () -> Unit) {
        reloj?.cancel()
        reloj = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(segundos = it.segundos + 1) }
                if (_uiState.value.segundos >= limite) {
                    alLlegarAlLimite()
                    break
                }
            }
        }
    }
}

/**
 * Pantalla "Notas de voz" (features notas-de-voz,
 * transcripcion-de-nota-voz, cola-offline-de-notas,
 * reintento-de-transcripcion-fallida y
 * sincronizacion-pendientes-al-reconectar).
 *
 * El socio graba una nota corta con el botón grande; la grabación queda
 * registrada como nota de voz con estado visible y se envía al backend para
 * transcribirla (el servidor no conserva el audio). Sin conexión, la nota
 * queda en la cola local (Room) en estado 'pendiente' con aviso visible y
 * puede reintentarse desde la cola. Si la transcripción falla, la nota queda
 * con error visible: se reintenta a mano o, si el audio no se puede usar, se
 * pide repetir el dictado.
 */
@Composable
fun PantallaNotasVoz(
    onVolver: () -> Unit,
    viewModel: NotasVozViewModel = hiltViewModel()
) {
    val contexto = LocalContext.current
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.abrir()
    }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            viewModel.empezarGrabacion()
        } else {
            viewModel.sinPermiso()
        }
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
            text = "Notas de voz",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Graba una nota corta para apuntar una labor.\nMáximo $LIMITE_SEGUNDOS_NOTA segundos.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (estado.grabando) {
                    viewModel.pararGrabacion()
                } else if (
                    ContextCompat.checkSelfPermission(
                        contexto, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.empezarGrabacion()
                } else {
                    lanzadorPermiso.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (estado.grabando) TextoError else VerdeOliva,
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (estado.grabando) "Parar" else "Grabar",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (estado.grabando) {
                "Grabando… ${estado.segundos} de $LIMITE_SEGUNDOS_NOTA segundos"
            } else {
                "Toca el botón para empezar a grabar."
            },
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = if (estado.grabando) TextoError else TextoMarron,
            textAlign = TextAlign.Center
        )
        estado.mensaje?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tus notas de voz",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeOliva
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (estado.notas.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Todavía no has grabado ninguna nota de voz.",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoMarron,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val pendientes = estado.notas.filter { it.estado == "pendiente" }
            val demas = estado.notas.filter { it.estado != "pendiente" }
            if (pendientes.isNotEmpty()) {
                // Cola local visible (feature cola-offline-de-notas).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, TextoError, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (pendientes.size == 1) {
                            "Cola: 1 nota pendiente de enviar"
                        } else {
                            "Cola: " + pendientes.size + " notas pendientes de enviar"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoError
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Están guardadas en el teléfono y no se pierden. Se enviarán cuando haya conexión o al tocar Reintentar.",
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = TextoMarron,
                        textAlign = TextAlign.Center
                    )
                    for (nota in pendientes) {
                        Spacer(modifier = Modifier.height(16.dp))
                        FilaNotaVoz(
                            nota,
                            alReintentar = { viewModel.transcribir(nota) },
                            alRepetir = { viewModel.empezarGrabacion() },
                            alGuardarBorrador = { borradorNuevo ->
                                viewModel.guardarBorrador(nota.id, borradorNuevo)
                            },
                            alConfirmarBorrador = { viewModel.confirmarBorrador(nota) },
                            alDescartarBorrador = { viewModel.descartarBorrador(nota) }
                        )
                    }
                }
            }
            if (demas.isNotEmpty()) {
                if (pendientes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for ((indice, nota) in demas.withIndex()) {
                        if (indice > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        FilaNotaVoz(
                            nota,
                            alReintentar = { viewModel.transcribir(nota) },
                            alRepetir = { viewModel.empezarGrabacion() },
                            alGuardarBorrador = { borradorNuevo ->
                                viewModel.guardarBorrador(nota.id, borradorNuevo)
                            },
                            alConfirmarBorrador = { viewModel.confirmarBorrador(nota) },
                            alDescartarBorrador = { viewModel.descartarBorrador(nota) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (estado.grabando) {
            Text(
                text = "Termina la grabación para volver.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron,
                textAlign = TextAlign.Center
            )
        } else {
            BotonSecundario(texto = "Volver al panel", alPulsar = onVolver)
        }
    }
}
