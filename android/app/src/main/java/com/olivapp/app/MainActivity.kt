package com.olivapp.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.olivapp.app.acceso.data.AlmacenSesiones
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.idCampanaDeLaFecha
import com.olivapp.app.parcelas.domain.DatosParcela
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.olivapp.app.core.formatos.formatearCantidad
import com.olivapp.app.core.navegacion.OlivApp
import com.olivapp.app.core.red.ServicioCuentas
import com.olivapp.app.core.ui.BotonPeligro
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.Desplegable
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.FondoCrema
import com.olivapp.app.core.ui.OpcionCategoria
import com.olivapp.app.core.ui.TextoError
import com.olivapp.app.core.ui.TextoMarron
import com.olivapp.app.core.ui.VerdeOliva
import com.olivapp.app.voz.data.AlmacenNotasVoz
import com.olivapp.app.voz.data.GrabadorNotas
import com.olivapp.app.voz.data.SincronizadorNotas
import com.olivapp.app.voz.domain.aplicarTranscripcion
import com.olivapp.app.voz.domain.camposFaltantesDeBorrador
import com.olivapp.app.voz.domain.etiquetaDeCampo
import com.olivapp.app.voz.domain.fechaIsoDeBorrador
import com.olivapp.app.voz.domain.lineasDeBorrador
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OlivApp() }
    }
}











/**
 * Pantalla de notas de voz (features grabacion-de-nota-voz,
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
fun PantallaNotasVoz(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val almacen = remember { AlmacenNotasVoz(contexto) }
    val grabador = remember { GrabadorNotas() }
    var notas by remember { mutableStateOf(almacen.notas()) }
    var grabando by remember { mutableStateOf(false) }
    var segundos by remember { mutableStateOf(0) }
    var ficheroActual by remember { mutableStateOf<File?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    val empezarGrabacion = {
        try {
            val fichero = almacen.nuevaRutaDeAudio()
            grabador.empezar(fichero)
            ficheroActual = fichero
            segundos = 0
            grabando = true
            mensaje = null
        } catch (e: Exception) {
            mensaje = "No se pudo empezar a grabar. Comprueba el permiso de micrófono."
        }
    }
    val transcribirNota = { nota: AlmacenNotasVoz.NotaDeVoz ->
        val token = AlmacenSesiones(contexto).token()
        if (token == null) {
            almacen.marcar(nota.id, "error", "")
            notas = almacen.notas()
            mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
        } else {
            almacen.marcar(nota.id, "transcribiendo", "")
            notas = almacen.notas()
            ServicioCuentas.transcribirNota(
                token,
                File(nota.fichero)
            ) { texto, borrador, error, sinConexion, audioInvalido ->
                mensaje = aplicarTranscripcion(
                    almacen,
                    nota.id,
                    texto,
                    borrador,
                    error,
                    sinConexion,
                    audioInvalido
                )
                notas = almacen.notas()
            }
        }
    }
    val pararGrabacion = {
        val duracion = grabador.parar()
        val fichero = ficheroActual
        ficheroActual = null
        grabando = false
        if (duracion >= 1 && fichero != null) {
            val nota = almacen.guardar(fichero, duracion)
            notas = almacen.notas()
            mensaje = "Nota de voz guardada. Transcribiendo…"
            transcribirNota(nota)
        } else {
            fichero?.delete()
            mensaje = "La grabación ha sido demasiado corta. Vuelve a intentarlo."
        }
    }

    // Al abrir la pantalla se sincroniza la cola: las notas pendientes se
    // envían en orden de grabación y sin duplicados.
    LaunchedEffect(Unit) {
        SincronizadorNotas.sincronizar(contexto)
    }

    // La cola también cambia sola al recuperar la conexión.
    DisposableEffect(Unit) {
        SincronizadorNotas.alCambiar = { notas = almacen.notas() }
        onDispose { SincronizadorNotas.alCambiar = null }
    }

    // Reloj de la grabación: se detiene sola al llegar al límite.
    LaunchedEffect(grabando) {
        while (grabando) {
            delay(1000)
            segundos += 1
            if (segundos >= LIMITE_SEGUNDOS_NOTA) {
                pararGrabacion()
                mensaje = "Has llegado a los $LIMITE_SEGUNDOS_NOTA segundos. Nota guardada y en transcripción."
            }
        }
    }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            empezarGrabacion()
        } else {
            mensaje = "Sin permiso de micrófono no se pueden grabar notas de voz."
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
                if (grabando) {
                    pararGrabacion()
                } else if (
                    ContextCompat.checkSelfPermission(
                        contexto, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    empezarGrabacion()
                } else {
                    lanzadorPermiso.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (grabando) TextoError else VerdeOliva,
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (grabando) "Parar" else "Grabar",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (grabando) {
                "Grabando… $segundos de $LIMITE_SEGUNDOS_NOTA segundos"
            } else {
                "Toca el botón para empezar a grabar."
            },
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = if (grabando) TextoError else TextoMarron,
            textAlign = TextAlign.Center
        )
        mensaje?.let {
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
        if (notas.isEmpty()) {
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
            val pendientes = notas.filter { it.estado == "pendiente" }
            val demas = notas.filter { it.estado != "pendiente" }
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
                            alReintentar = { transcribirNota(nota) },
                            alRepetir = { empezarGrabacion() },
                            alGuardarBorrador = { borradorNuevo ->
                                almacen.actualizarBorrador(nota.id, borradorNuevo)
                                notas = almacen.notas()
                            },
                            alConfirmarBorrador = {
                                // La nota queda confirmada: su borrador ya se ha
                                // convertido en labor de Mi cuaderno.
                                almacen.marcar(
                                    nota.id, "confirmada", nota.texto, nota.borrador
                                )
                                notas = almacen.notas()
                            },
                            alDescartarBorrador = {
                                // La nota queda descartada: el socio rechaza el
                                // borrador y no se crea ninguna labor.
                                almacen.marcar(
                                    nota.id, "descartada", nota.texto, nota.borrador
                                )
                                notas = almacen.notas()
                            }
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
                            alReintentar = { transcribirNota(nota) },
                            alRepetir = { empezarGrabacion() },
                            alGuardarBorrador = { borradorNuevo ->
                                almacen.actualizarBorrador(nota.id, borradorNuevo)
                                notas = almacen.notas()
                            },
                            alConfirmarBorrador = {
                                // La nota queda confirmada: su borrador ya se ha
                                // convertido en labor de Mi cuaderno.
                                almacen.marcar(
                                    nota.id, "confirmada", nota.texto, nota.borrador
                                )
                                notas = almacen.notas()
                            },
                            alDescartarBorrador = {
                                // La nota queda descartada: el socio rechaza el
                                // borrador y no se crea ninguna labor.
                                almacen.marcar(
                                    nota.id, "descartada", nota.texto, nota.borrador
                                )
                                notas = almacen.notas()
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (grabando) {
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

/** Límite de duración de una nota de voz (decisión de esta feature). */
private const val LIMITE_SEGUNDOS_NOTA = 60

/** Una nota de voz de la cola local: cuándo se grabó, cuánto dura, su estado y su texto. */
@Composable
fun FilaNotaVoz(
    nota: AlmacenNotasVoz.NotaDeVoz,
    alReintentar: () -> Unit,
    alRepetir: () -> Unit,
    alGuardarBorrador: (String) -> Unit,
    alConfirmarBorrador: () -> Unit,
    alDescartarBorrador: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = nota.cuando + " · " + nota.segundos + " s",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = nota.estadoVisible,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = if (nota.estado == "error" || nota.estado == "audio_invalido") {
                TextoError
            } else {
                VerdeOliva
            }
        )
        if (nota.texto.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nota.texto,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron
            )
        }
        if (nota.estado == "transcrita" && nota.borrador.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            BorradorIA(
                nota.borrador, nota.id, alGuardarBorrador, alConfirmarBorrador,
                alDescartarBorrador
            )
        }
        if (nota.estado == "error" || nota.estado == "pendiente") {
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(texto = "Reintentar", alPulsar = alReintentar)
        }
        if (nota.estado == "audio_invalido") {
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(texto = "Grabar otra vez", alPulsar = alRepetir)
        }
    }
}

/**
 * Borrador IA de una nota transcrita (features borrador-ia-desde-transcripcion,
 * completar-borrador-por-preguntas, confirmacion-de-borrador y
 * borrador-sin-parcela y fecha-relativa-en-borrador).
 *
 * Muestra la ficha que propone la IA (fecha, parcela, categoría y campos de
 * la categoría) esperando la confirmación del socio. Si la IA no reconoció la
 * categoría, el borrador se rechaza con un mensaje claro.
 *
 * Cuando faltan campos obligatorios, el asistente pregunta por ellos y el
 * socio responde por voz: cada respuesta completa la ficha y trae la
 * siguiente pregunta. Mientras falte algún campo, el borrador no se puede
 * confirmar (el botón "Confirmar" está deshabilitado).
 *
 * Con la ficha completa, "Confirmar" crea la labor en Mi cuaderno y la nota
 * queda confirmada. La confirmación lleva el id de la nota y el backend la
 * hace idempotente: confirmar dos veces no crea dos labores (una Nota de voz
 * produce como máximo una Labor).
 *
 * "Descartar" rechaza el borrador (features descarte-de-borrador): con
 * confirmación previa, porque no hay papelera que lo recupere, la nota queda
 * descartada y no se crea ninguna labor.
 *
 * Si la IA no identifica la parcela, la ficha deja la parcela vacía y el socio
 * elige una de su lista (feature borrador-sin-parcela): hasta entonces
 * "Confirmar" sigue deshabilitado.
 */
@Composable
fun BorradorIA(
    json: String,
    notaId: String,
    alGuardar: (String) -> Unit,
    alConfirmar: () -> Unit,
    alDescartar: () -> Unit
) {
    val contexto = LocalContext.current
    val borrador = try {
        JSONObject(json)
    } catch (e: Exception) {
        null
    }
    var preguntarDescarte by remember { mutableStateOf(false) }

    // Confirmación antes del descarte (no hay papelera que lo recupere).
    if (preguntarDescarte) {
        AlertDialog(
            onDismissRequest = { preguntarDescarte = false },
            title = {
                Text(
                    text = "¿Descartar este borrador?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoMarron
                )
            },
            text = {
                Text(
                    text = "La nota quedará descartada y no se creará ninguna labor. " +
                        "No se puede deshacer.",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoMarron
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    preguntarDescarte = false
                    alDescartar()
                }) {
                    Text(text = "Descartar", fontSize = 20.sp, color = TextoError)
                }
            },
            dismissButton = {
                TextButton(onClick = { preguntarDescarte = false }) {
                    Text(text = "Cancelar", fontSize = 20.sp, color = VerdeOliva)
                }
            }
        )
    }

    if (borrador == null) return
    if (borrador.optBoolean("rechazado")) {
        Text(
            text = "No se ha reconocido la categoría de la labor. Solo hay tres: " +
                "tratamiento fitosanitario, riego y cosecha. Vuelve a dictar la nota.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoError
        )
        Spacer(modifier = Modifier.height(8.dp))
        BotonPeligro(texto = "Descartar", alPulsar = { preguntarDescarte = true })
        return
    }
    val faltantes = camposFaltantesDeBorrador(borrador)
    // La parcela del borrador debe estar en la lista del socio (feature
    // borrador-sin-parcela): si la IA no la identificó, queda vacía y hay que
    // elegirla antes de poder confirmar.
    val parcelaResuelta = borrador.optString("parcela").isNotBlank()

    // fecha-relativa-en-borrador: la fecha concreta del borrador se puede
    // corregir en el calendario y solo se puede confirmar si cae en la campaña
    // actual: las campañas cerradas son de solo consulta.
    var eligiendoFecha by remember { mutableStateOf(false) }
    var campanasDeLaCuenta by remember { mutableStateOf<List<DatosCampanaResumen>>(emptyList()) }
    LaunchedEffect(Unit) {
        val token = AlmacenSesiones(contexto).token()
        if (token != null) {
            ServicioCuentas.cargarCampanas(token) { lista, _ ->
                if (lista != null) {
                    campanasDeLaCuenta = lista
                }
            }
        }
    }
    val fechaResuelta = fechaIsoDeBorrador(borrador)
    val campanaDeLaFecha = if (fechaResuelta.isEmpty()) {
        null
    } else {
        campanasDeLaCuenta.firstOrNull { it.id == idCampanaDeLaFecha(fechaResuelta) }
    }
    val campanaActual = campanasDeLaCuenta.firstOrNull { it.estado == "actual" }
    val fechaAdmitida = fechaResuelta.isEmpty() || campanasDeLaCuenta.isEmpty() ||
        (campanaDeLaFecha != null && campanaDeLaFecha.estado == "actual")
    val avisoDeFecha = when {
        campanaDeLaFecha == null ->
            "Esta fecha no corresponde a ninguna campaña conocida. " +
                "Corrige la fecha para poder confirmar."
        campanaActual == null ->
            "Esta fecha es de la Campaña ${campanaDeLaFecha.nombre} " +
                "(cerrada: solo consulta). Corrige la fecha para poder confirmar."
        else ->
            "Esta fecha es de la Campaña ${campanaDeLaFecha.nombre} " +
                "(cerrada: solo consulta). Las labores se apuntan en la " +
                "Campaña ${campanaActual.nombre}. Corrige la fecha para poder confirmar."
    }
    var parcelasDeLaCuenta by remember { mutableStateOf<List<String>>(emptyList()) }
    var cargandoParcelas by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!parcelaResuelta) {
            cargandoParcelas = true
            val tokenDeParcelas = AlmacenSesiones(contexto).token()
            if (tokenDeParcelas == null) {
                cargandoParcelas = false
            } else {
                ServicioCuentas.cargarParcelas(tokenDeParcelas) { lista, _ ->
                    parcelasDeLaCuenta = lista?.map { it.nombre } ?: emptyList()
                    cargandoParcelas = false
                }
            }
        }
    }
    val grabador = remember { GrabadorNotas() }
    var grabando by remember { mutableStateOf(false) }
    var segundos by remember { mutableStateOf(0) }
    var ficheroRespuesta by remember { mutableStateOf<File?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var confirmando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var avisoConfirmar by remember { mutableStateOf<String?>(null) }
    var ultimaRespuesta by remember { mutableStateOf<String?>(null) }

    fun enviarRespuesta() {
        val fichero = ficheroRespuesta ?: return
        val token = AlmacenSesiones(contexto).token()
        if (token == null) {
            aviso = "Sesión caducada o no válida. Vuelve a iniciar sesión."
            return
        }
        enviando = true
        aviso = null
        ServicioCuentas.completarBorrador(token, fichero, json) {
                texto, borradorNuevo, error, sinConexion, audioInvalido ->
            enviando = false
            if (texto != null) {
                // La respuesta completa la ficha y trae la siguiente pregunta.
                ficheroRespuesta?.delete()
                ficheroRespuesta = null
                ultimaRespuesta = texto
                alGuardar(borradorNuevo)
            } else if (audioInvalido) {
                // El audio no se puede usar: hay que volver a responder.
                ficheroRespuesta?.delete()
                ficheroRespuesta = null
                aviso = error
            } else {
                // Sin conexión o fallo del servicio: la respuesta grabada se
                // conserva y se reintenta sin tener que dictarla otra vez.
                aviso = error
            }
        }
    }

    fun confirmar() {
        val token = AlmacenSesiones(contexto).token()
        if (token == null) {
            avisoConfirmar = "Sesión caducada o no válida. Vuelve a iniciar sesión."
            return
        }
        confirmando = true
        avisoConfirmar = null
        val nombreParcela = borrador.optString("parcela")
        ServicioCuentas.cargarParcelas(token) { parcelas, _ ->
            // La ficha guarda el nombre de la parcela; la labor se crea con su id.
            val parcela = parcelas?.firstOrNull {
                it.nombre.equals(nombreParcela, ignoreCase = true)
            }
            if (parcela == null) {
                confirmando = false
                avisoConfirmar = "No se ha encontrado la parcela del borrador en " +
                    "tu lista de parcelas. Elígela antes de confirmar."
                return@cargarParcelas
            }
            val cuerpo = JSONObject()
                .put("fecha", borrador.optString("fecha"))
                .put("parcela_id", parcela.id)
                .put("categoria", borrador.optString("categoria"))
                .put("campos", borrador.optJSONObject("campos") ?: JSONObject())
                .put("observaciones", borrador.optString("observaciones"))
                // Una Nota de voz produce como máximo una Labor: el id de la
                // nota hace la creación idempotente en el backend.
                .put("nota_id", notaId)
            ServicioCuentas.guardarLabor(token, cuerpo) { campana, texto ->
                confirmando = false
                if (campana != null) {
                    alConfirmar()
                } else {
                    avisoConfirmar = texto
                }
            }
        }
    }

    val empezarRespuesta = {
        try {
            val carpeta = File(contexto.filesDir, "respuestas-borrador")
            carpeta.mkdirs()
            val fichero = File(carpeta, "respuesta-" + System.currentTimeMillis() + ".m4a")
            grabador.empezar(fichero)
            ficheroRespuesta = fichero
            segundos = 0
            grabando = true
            aviso = null
            ultimaRespuesta = null
        } catch (e: Exception) {
            aviso = "No se pudo empezar a grabar. Comprueba el permiso de micrófono."
        }
    }
    val pararRespuesta = {
        val duracion = grabador.parar()
        grabando = false
        val fichero = ficheroRespuesta
        if (duracion >= 1 && fichero != null) {
            enviarRespuesta()
        } else {
            fichero?.delete()
            ficheroRespuesta = null
            aviso = "La grabación ha sido demasiado corta. Vuelve a intentarlo."
        }
    }

    // Reloj de la respuesta: se detiene sola al llegar al límite.
    LaunchedEffect(grabando) {
        while (grabando) {
            delay(1000)
            segundos += 1
            if (segundos >= LIMITE_SEGUNDOS_RESPUESTA) {
                pararRespuesta()
            }
        }
    }

    // Si se abandona la conversación con una respuesta grabada sin enviar, el
    // audio no se queda en el teléfono: la pregunta sigue ahí y se vuelve a
    // responder.
    DisposableEffect(Unit) {
        onDispose {
            if (!enviando) {
                ficheroRespuesta?.delete()
                ficheroRespuesta = null
            }
        }
    }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            empezarRespuesta()
        } else {
            aviso = "Sin permiso de micrófono no se puede responder por voz."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Borrador IA",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeOliva
        )
        Spacer(modifier = Modifier.height(8.dp))
        for ((etiqueta, valor) in lineasDeBorrador(borrador)) {
            Text(
                text = "$etiqueta: $valor",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (!parcelaResuelta) {
            // feature borrador-sin-parcela: la IA no identificó la parcela y el
            // socio elige una de su lista antes de poder confirmar.
            Text(
                text = "Elige la parcela de esta labor:",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextoError
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (cargandoParcelas) {
                Text(
                    text = "Cargando tus parcelas…",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoMarron
                )
            } else if (parcelasDeLaCuenta.isEmpty()) {
                Text(
                    text = "Todavía no has creado ninguna parcela. Crea una parcela " +
                        "para poder apuntar esta labor.",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoError
                )
            } else {
                Desplegable(
                    etiqueta = "Parcela",
                    valor = "",
                    opciones = parcelasDeLaCuenta,
                    alElegir = { elegida ->
                        // La parcela elegida queda guardada en la ficha.
                        alGuardar(borrador.put("parcela", elegida).toString())
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        // fecha-relativa-en-borrador: sin fecha concreta hay que elegirla en el
        // calendario, y una fecha de otra campaña se corrige antes de confirmar.
        if (fechaResuelta.isEmpty()) {
            Text(
                text = "Elige la fecha de esta labor:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextoError
            )
            Spacer(modifier = Modifier.height(8.dp))
            BotonPrincipal(texto = "Elegir fecha", alPulsar = { eligiendoFecha = true })
            Spacer(modifier = Modifier.height(12.dp))
        } else if (!fechaAdmitida) {
            Text(
                text = avisoDeFecha,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextoError
            )
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(texto = "Cambiar la fecha", alPulsar = { eligiendoFecha = true })
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (eligiendoFecha) {
            CalendarioDeFecha(
                alElegir = { elegida ->
                    eligiendoFecha = false
                    alGuardar(borrador.put("fecha", elegida).toString())
                },
                alCerrar = { eligiendoFecha = false }
            )
        }
        val completo = faltantes.isEmpty() && parcelaResuelta &&
            fechaResuelta.isNotEmpty() && fechaAdmitida
        if (completo) {
            Text(
                text = "Borrador completo. Ya se puede confirmar.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(8.dp))
            BotonPrincipal(
                texto = if (confirmando) "Confirmando…" else "Confirmar",
                habilitado = !grabando && !enviando && !confirmando,
                alPulsar = { confirmar() }
            )
            avisoConfirmar?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoError
                )
            }
        } else {
            if (faltantes.isNotEmpty()) {
                Text(
                    text = "Faltan datos por responder: " +
                        faltantes.joinToString(", ") { etiquetaDeCampo(it) } + ".",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoError
                )
                val pregunta = borrador.optString("pregunta")
                if (pregunta.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "El asistente te pregunta:",
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeOliva
                    )
                    Text(
                        text = pregunta,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = TextoMarron
                    )
                }
                ultimaRespuesta?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tu respuesta: $it",
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = TextoMarron
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (grabando) {
                    Text(
                        text = "Grabando tu respuesta… $segundos de " +
                            "$LIMITE_SEGUNDOS_RESPUESTA segundos",
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = TextoError
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BotonPeligro(texto = "Parar", alPulsar = pararRespuesta)
                } else if (enviando) {
                    Text(
                        text = "Enviando tu respuesta…",
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = VerdeOliva
                    )
                } else if (ficheroRespuesta != null) {
                    // Respuesta ya grabada que aún no se ha podido enviar.
                    BotonPrincipal(texto = "Reintentar", alPulsar = { enviarRespuesta() })
                    Spacer(modifier = Modifier.height(8.dp))
                    BotonSecundario(
                        texto = "Responder otra vez",
                        alPulsar = {
                            ficheroRespuesta?.delete()
                            ficheroRespuesta = null
                            empezarRespuesta()
                        }
                    )
                } else {
                    BotonPrincipal(
                        texto = "Responder por voz",
                        alPulsar = {
                            if (
                                ContextCompat.checkSelfPermission(
                                    contexto, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                empezarRespuesta()
                            } else {
                                lanzadorPermiso.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                }
                aviso?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = TextoError
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Mientras falte un campo obligatorio o la parcela, el borrador no
            // se puede confirmar.
            BotonPrincipal(texto = "Confirmar", habilitado = false, alPulsar = {})
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    faltantes.isNotEmpty() && !parcelaResuelta && fechaResuelta.isEmpty() ->
                        "Faltan datos por responder y hay que elegir la parcela y la fecha " +
                            "para poder confirmar."
                    faltantes.isNotEmpty() && !parcelaResuelta && !fechaAdmitida ->
                        "Faltan datos por responder y hay que elegir la parcela y corregir " +
                            "la fecha para poder confirmar."
                    faltantes.isNotEmpty() && !parcelaResuelta ->
                        "Faltan datos por responder y hay que elegir la parcela " +
                            "para poder confirmar."
                    faltantes.isNotEmpty() && fechaResuelta.isEmpty() ->
                        "Faltan datos por responder y hay que elegir la fecha " +
                            "para poder confirmar."
                    faltantes.isNotEmpty() && !fechaAdmitida ->
                        "Faltan datos por responder y hay que corregir la fecha " +
                            "para poder confirmar."
                    faltantes.isNotEmpty() ->
                        "Faltan datos por responder para poder confirmar."
                    !parcelaResuelta && fechaResuelta.isEmpty() ->
                        "Elige la parcela y la fecha de esta labor para poder confirmar."
                    !parcelaResuelta && !fechaAdmitida ->
                        "Elige la parcela y corrige la fecha de esta labor " +
                            "para poder confirmar."
                    !parcelaResuelta ->
                        "Elige la parcela de esta labor para poder confirmar."
                    fechaResuelta.isEmpty() ->
                        "Elige la fecha de esta labor para poder confirmar."
                    else ->
                        "Corrige la fecha de esta labor para poder confirmar."
                },
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron
            )
        }
        // El socio puede rechazar el borrador en cualquier momento, con la
        // ficha completa o incompleta: la nota queda descartada y no se crea
        // ninguna labor.
        Spacer(modifier = Modifier.height(12.dp))
        BotonPeligro(texto = "Descartar", alPulsar = { preguntarDescarte = true })
    }
}

/** Límite de duración de la respuesta por voz a una pregunta del asistente. */
private const val LIMITE_SEGUNDOS_RESPUESTA = 20



/** Calendario para elegir la fecha concreta de una labor (AAAA-MM-DD). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioDeFecha(alElegir: (String) -> Unit, alCerrar: () -> Unit) {
    val estado = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    DatePickerDialog(
        onDismissRequest = alCerrar,
        confirmButton = {
            TextButton(onClick = {
                val elegida = estado.selectedDateMillis ?: System.currentTimeMillis()
                alElegir(
                    java.time.Instant.ofEpochMilli(elegida)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate()
                        .toString()
                )
            }) {
                Text(text = "Aceptar", fontSize = 20.sp, color = VerdeOliva)
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) {
                Text(text = "Cancelar", fontSize = 20.sp, color = VerdeOliva)
            }
        }
    ) {
        DatePicker(state = estado)
    }
}







/**
 * Cola local de notas de voz (features transcripcion-de-nota-voz y
 * cola-offline-de-notas).
 *
 * La cola vive en Room (SQLite del dispositivo) y el audio en el
 * almacenamiento de la app hasta que la transcripción lo envíe al backend
 * (el servidor no conserva el audio). Sin conexión, la nota se guarda en
 * estado 'pendiente' y no se pierde ni al cerrar la app.
 */











/**
 * Simulador de liquidación en limpio (feature calculo-de-liquidacion-en-limpio):
 * el socio introduce kilos de aceituna y rendimiento graso y ve el aceite neto
 * estimado (kilos × rendimiento − ajuste industrial), claramente etiquetado
 * como estimación orientativa. La simulación es bajo demanda y no se guarda:
 * no compite con las cifras oficiales de la cooperativa (CONTEXT.md).
 *
 * El ajuste industrial es configurable (feature ajuste-industrial-configurable):
 * el campo arranca en el 10 % por defecto (ADR 0007) y, si ya hay un resultado,
 * cambiarlo recalcula el aceite neto al instante. Cambiar kilos o rendimiento
 * sigue borrando el resultado: el cálculo va bajo demanda con "Simular".
 */
@Composable
fun PantallaSimulador(onVolver: () -> Unit) {
    var kilos by remember { mutableStateOf("") }
    var rendimiento by remember { mutableStateOf("") }
    var ajuste by remember { mutableStateOf("10") }
    var resultado by remember { mutableStateOf<ResultadoSimulacion?>(null) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Liquidación en limpio",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Simula tu liquidación en limpio con los kilos de aceituna " +
                "y el rendimiento graso.\nEs una estimación orientativa, no la " +
                "liquidación oficial de la cooperativa.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CampoDeTexto(
            etiqueta = "Kilos de aceituna",
            valor = kilos,
            alCambiar = {
                kilos = it
                resultado = null
            },
            tipo = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Rendimiento graso (%)",
            valor = rendimiento,
            alCambiar = {
                rendimiento = it
                resultado = null
            },
            tipo = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Ajuste industrial (%)",
            valor = ajuste,
            alCambiar = {
                ajuste = it
                // Recálculo al instante: si ya hay resultado y el nuevo ajuste
                // es un porcentaje válido, el aceite neto se recalcula con él.
                val ajusteNuevo = numeroDeCampo(it)
                val anterior = resultado
                if (anterior != null && ajusteNuevo != null &&
                    ajusteNuevo >= 0.0 && ajusteNuevo <= 100.0
                ) {
                    resultado = recalcularConAjuste(anterior, ajusteNuevo)
                }
            },
            tipo = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(
            texto = "Simular",
            alPulsar = {
                error = ""
                val kilosSimulados = numeroDeCampo(kilos)
                val rendimientoSimulado = numeroDeCampo(rendimiento)
                val ajusteSimulado = numeroDeCampo(ajuste)
                when {
                    kilosSimulados == null || kilosSimulados <= 0.0 -> {
                        resultado = null
                        error = "Introduce los kilos de aceituna, mayores que cero."
                    }
                    rendimientoSimulado == null ||
                        rendimientoSimulado <= 0.0 || rendimientoSimulado > 100.0 -> {
                        resultado = null
                        error = "Introduce el rendimiento graso como porcentaje " +
                            "entre 0 y 100 (por ejemplo, 20)."
                    }
                    ajusteSimulado == null ||
                        ajusteSimulado < 0.0 || ajusteSimulado > 100.0 -> {
                        resultado = null
                        error = "Introduce el ajuste industrial como porcentaje " +
                            "entre 0 y 100 (por ejemplo, 10)."
                    }
                    else -> resultado =
                        simularLiquidacion(
                            kilosSimulados, rendimientoSimulado, ajusteSimulado
                        )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (error.isNotEmpty()) {
            Text(
                text = error,
                fontSize = 20.sp,
                color = TextoError,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        resultado?.let {
            Text(
                text = "Aceite neto estimado",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextoMarron
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatearCantidad(it.aceiteNeto) + " kg",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilaDato(
                etiqueta = "Aceite según el rendimiento",
                valor = formatearCantidad(it.aceiteSegunRendimiento) + " kg"
            )
            FilaDato(
                etiqueta = "Ajuste industrial (" +
                    formatearCantidad(it.ajustePorcentaje) + " %)",
                valor = "-" + formatearCantidad(it.ajusteIndustrial) + " kg"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Estimación orientativa: la liquidación oficial es la " +
                    "de la cooperativa.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        BotonSecundario(texto = "Volver al panel", alPulsar = onVolver)
    }
}

/** Resultado de una simulación de liquidación en limpio (en kilos de aceite). */
class ResultadoSimulacion(
    val aceiteSegunRendimiento: Double,
    val ajustePorcentaje: Double,
    val ajusteIndustrial: Double,
    val aceiteNeto: Double
)

/**
 * Liquidación en limpio: kilos × rendimiento graso − ajuste industrial (única
 * deducción del MVP). El ajuste se expresa en porcentaje (10 % por defecto,
 * ADR 0007) y es configurable por el socio. No se guarda en ningún sitio.
 */
fun simularLiquidacion(
    kilos: Double,
    rendimiento: Double,
    ajustePorcentaje: Double = 10.0
): ResultadoSimulacion {
    val aceite = kilos * rendimiento / 100.0
    val merma = aceite * ajustePorcentaje / 100.0
    return ResultadoSimulacion(aceite, ajustePorcentaje, merma, aceite - merma)
}

/**
 * Recálculo al instante del resultado con un ajuste industrial nuevo: el aceite
 * según el rendimiento no cambia, solo la deducción que se le aplica.
 */
fun recalcularConAjuste(
    anterior: ResultadoSimulacion,
    ajustePorcentaje: Double
): ResultadoSimulacion {
    val merma = anterior.aceiteSegunRendimiento * ajustePorcentaje / 100.0
    return ResultadoSimulacion(
        anterior.aceiteSegunRendimiento,
        ajustePorcentaje,
        merma,
        anterior.aceiteSegunRendimiento - merma
    )
}

/**
 * Número tecleado por el socio: acepta coma o punto decimal ("20,5" o "20.5")
 * y los puntos de millar ("10.958"). Vacío o ilegible devuelve null.
 */
fun numeroDeCampo(texto: String): Double? {
    val limpio = texto.trim().replace(" ", "")
    if (limpio.isEmpty()) return null
    val normalizado = if (limpio.contains(',')) {
        limpio.replace(".", "").replace(',', '.')
    } else {
        val partes = limpio.split('.')
        if (partes.size > 1 && partes.last().length == 3) {
            limpio.replace(".", "")
        } else {
            limpio
        }
    }
    return normalizado.toDoubleOrNull()
}







