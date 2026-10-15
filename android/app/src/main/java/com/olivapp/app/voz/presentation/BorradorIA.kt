package com.olivapp.app.voz.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.campana.domain.CampanasRepositorio
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.idCampanaDeLaFecha
import com.olivapp.app.core.ui.BotonPeligro
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.Desplegable
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.TextoBoton
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.cuaderno.domain.LaboresRepositorio
import com.olivapp.app.parcelas.domain.ParcelasRepositorio
import com.olivapp.app.voz.domain.GrabadorDeNotas
import com.olivapp.app.voz.domain.NotasRepositorio
import com.olivapp.app.voz.domain.camposFaltantesDeBorrador
import com.olivapp.app.voz.domain.etiquetaDeCampo
import com.olivapp.app.voz.domain.fechaIsoDeBorrador
import com.olivapp.app.voz.domain.lineasDeBorrador
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import com.olivapp.app.R

/** Límite de duración de la respuesta por voz a una pregunta del asistente. */
private const val LIMITE_SEGUNDOS_RESPUESTA = 20

/** Estado visible del borrador IA de una nota: su ficha y la conversación. */
data class BorradorUiState(
    val fichaJson: String = "",
    val campanas: List<DatosCampanaResumen> = emptyList(),
    val parcelas: List<String> = emptyList(),
    val cargandoParcelas: Boolean = false,
    val eligiendoFecha: Boolean = false,
    val preguntarDescarte: Boolean = false,
    val grabando: Boolean = false,
    val segundos: Int = 0,
    val hayRespuestaSinEnviar: Boolean = false,
    val enviando: Boolean = false,
    val confirmando: Boolean = false,
    val aviso: String? = null,
    val avisoConfirmar: String? = null,
    val ultimaRespuesta: String? = null
)

/**
 * Cerebro del borrador IA de una nota (un ViewModel por nota dentro de la
 * visita a la pantalla de notas): guarda la ficha, responde por voz a las
 * preguntas del asistente y confirma la labor.
 */
@HiltViewModel
class BorradorViewModel @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val sesion: SesionRepositorio,
    private val campanasRepo: CampanasRepositorio,
    private val parcelasRepo: ParcelasRepositorio,
    private val laboresRepo: LaboresRepositorio,
    private val notasRepo: NotasRepositorio,
    private val grabador: GrabadorDeNotas
) : ViewModel() {

    private var notaId: String = ""
    private var ficha: JSONObject? = null
    private var ficheroRespuesta: File? = null
    private var reloj: Job? = null
    private var preparada = false

    private val _uiState = MutableStateFlow(BorradorUiState())
    val uiState: StateFlow<BorradorUiState> = _uiState.asStateFlow()

    /**
     * Prepara el panel con la ficha de la nota la primera vez de la visita (el
     * estado sobrevive a la rotación) y carga las campañas y, si hace falta,
     * las parcelas del socio.
     */
    fun preparar(notaId: String, fichaJson: String) {
        if (preparada) return
        preparada = true
        this.notaId = notaId
        this.ficha = aFicha(fichaJson)
        _uiState.update { it.copy(fichaJson = fichaJson) }

        // fecha-relativa-en-borrador: la fecha concreta del borrador se puede
        // corregir en el calendario y solo se puede confirmar si cae en la
        // campaña actual: las campañas cerradas son de solo consulta.
        val token = sesion.token()
        if (token != null) {
            campanasRepo.cargarCampanas(token) { lista, _ ->
                if (lista != null) {
                    _uiState.update { it.copy(campanas = lista) }
                }
            }
        }

        // La parcela del borrador debe estar en la lista del socio (feature
        // borrador-sin-parcela): si la IA no la identificó, queda vacía y hay
        // que elegirla antes de poder confirmar.
        val parcelaResuelta = ficha?.optString("parcela")?.isNotBlank() == true
        if (!parcelaResuelta) {
            _uiState.update { it.copy(cargandoParcelas = true) }
            val tokenDeParcelas = sesion.token()
            if (tokenDeParcelas == null) {
                _uiState.update { it.copy(cargandoParcelas = false) }
            } else {
                parcelasRepo.cargarParcelas(tokenDeParcelas) { lista, _ ->
                    _uiState.update {
                        it.copy(
                            parcelas = lista?.map { parcela -> parcela.nombre } ?: emptyList(),
                            cargandoParcelas = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Al salir de la conversación, una grabación de respuesta en curso se para
     * y libera el micrófono (si no, la siguiente grabación falla con un aviso
     * engañoso de permisos) y un audio sin enviar no se queda en el teléfono:
     * la pregunta sigue ahí y se vuelve a responder.
     */
    fun alSalir() {
        reloj?.cancel()
        if (_uiState.value.grabando) {
            grabador.parar()
            _uiState.update { it.copy(grabando = false) }
        }
        if (!_uiState.value.enviando) {
            ficheroRespuesta?.delete()
            ficheroRespuesta = null
            _uiState.update { it.copy(hayRespuestaSinEnviar = false) }
        }
    }

    override fun onCleared() {
        alSalir()
    }

    /** La parcela elegida queda guardada en la ficha. */
    fun elegirParcela(nombre: String, alGuardar: (String) -> Unit) {
        val nueva = ficha?.put("parcela", nombre)?.toString() ?: return
        _uiState.update { it.copy(fichaJson = nueva) }
        alGuardar(nueva)
    }

    fun abrirCalendario() {
        _uiState.update { it.copy(eligiendoFecha = true) }
    }

    fun cerrarCalendario() {
        _uiState.update { it.copy(eligiendoFecha = false) }
    }

    fun elegirFecha(fecha: String, alGuardar: (String) -> Unit) {
        val nueva = ficha?.put("fecha", fecha)?.toString() ?: return
        _uiState.update { it.copy(eligiendoFecha = false, fichaJson = nueva) }
        alGuardar(nueva)
    }

    /** El socio pide descartar el borrador: se le pide confirmación antes. */
    fun pedirDescartar() {
        _uiState.update { it.copy(preguntarDescarte = true) }
    }

    fun cancelarDescartar() {
        _uiState.update { it.copy(preguntarDescarte = false) }
    }

    /** Descarte confirmado: la nota queda descartada y no se crea ninguna labor. */
    fun confirmarDescartar(alDescartar: () -> Unit) {
        _uiState.update { it.copy(preguntarDescarte = false) }
        alDescartar()
    }

    fun empezarRespuesta(alGuardar: (String) -> Unit) {
        if (_uiState.value.grabando) return
        try {
            val carpeta = File(contexto.filesDir, "respuestas-borrador")
            carpeta.mkdirs()
            val fichero = File(carpeta, "respuesta-" + System.currentTimeMillis() + ".m4a")
            grabador.empezar(fichero)
            ficheroRespuesta = fichero
            _uiState.update {
                it.copy(
                    segundos = 0,
                    grabando = true,
                    hayRespuestaSinEnviar = true,
                    aviso = null,
                    ultimaRespuesta = null
                )
            }
            // Reloj de la respuesta: se detiene sola al llegar al límite.
            reloj?.cancel()
            reloj = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _uiState.update { it.copy(segundos = it.segundos + 1) }
                    if (_uiState.value.segundos >= LIMITE_SEGUNDOS_RESPUESTA) {
                        pararRespuesta(alGuardar)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(aviso = "No se pudo empezar a grabar. Comprueba el permiso de micrófono.")
            }
        }
    }

    fun pararRespuesta(alGuardar: (String) -> Unit) {
        val duracion = grabador.parar()
        reloj?.cancel()
        _uiState.update { it.copy(grabando = false) }
        val fichero = ficheroRespuesta
        if (duracion >= 1 && fichero != null) {
            enviarRespuesta(alGuardar)
        } else {
            fichero?.delete()
            ficheroRespuesta = null
            _uiState.update {
                it.copy(
                    hayRespuestaSinEnviar = false,
                    aviso = "La grabación ha sido demasiado corta. Vuelve a intentarlo."
                )
            }
        }
    }

    /** Respuesta grabada que aún no se ha podido enviar: se tira y se responde
     * otra vez ("Responder otra vez"). */
    fun reiniciarRespuesta(alGuardar: (String) -> Unit) {
        ficheroRespuesta?.delete()
        ficheroRespuesta = null
        _uiState.update { it.copy(hayRespuestaSinEnviar = false) }
        empezarRespuesta(alGuardar)
    }

    fun enviarRespuesta(alGuardar: (String) -> Unit) {
        val fichero = ficheroRespuesta ?: return
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(aviso = "Sesión caducada o no válida. Vuelve a iniciar sesión.")
            }
            return
        }
        _uiState.update { it.copy(enviando = true, aviso = null) }
        notasRepo.completarBorrador(token, fichero, _uiState.value.fichaJson) {
                texto, borradorNuevo, error, sinConexion, audioInvalido ->
            _uiState.update { it.copy(enviando = false) }
            if (texto != null) {
                // La respuesta completa la ficha y trae la siguiente pregunta.
                ficheroRespuesta?.delete()
                ficheroRespuesta = null
                ficha = aFicha(borradorNuevo)
                _uiState.update {
                    it.copy(
                        hayRespuestaSinEnviar = false,
                        ultimaRespuesta = texto,
                        fichaJson = borradorNuevo
                    )
                }
                alGuardar(borradorNuevo)
            } else if (audioInvalido) {
                // El audio no se puede usar: hay que volver a responder.
                ficheroRespuesta?.delete()
                ficheroRespuesta = null
                _uiState.update { it.copy(hayRespuestaSinEnviar = false, aviso = error) }
            } else {
                // Sin conexión o fallo del servicio: la respuesta grabada se
                // conserva y se reintenta sin tener que dictarla otra vez.
                _uiState.update { it.copy(aviso = error) }
            }
        }
    }

    /** Con la ficha completa, "Confirmar" crea la labor en Mi cuaderno y la
     * nota queda confirmada. La confirmación lleva el id de la nota y el
     * backend la hace idempotente: confirmar dos veces no crea dos labores
     * (una Nota de voz produce como máximo una Labor). */
    fun confirmar(alConfirmar: () -> Unit) {
        val token = sesion.token()
        if (token == null) {
            _uiState.update {
                it.copy(avisoConfirmar = "Sesión caducada o no válida. Vuelve a iniciar sesión.")
            }
            return
        }
        val borrador = ficha ?: return
        _uiState.update { it.copy(confirmando = true, avisoConfirmar = null) }
        val nombreParcela = borrador.optString("parcela")
        parcelasRepo.cargarParcelas(token) { parcelas, _ ->
            // La ficha guarda el nombre de la parcela; la labor se crea con su id.
            val parcela = parcelas?.firstOrNull {
                it.nombre.equals(nombreParcela, ignoreCase = true)
            }
            if (parcela == null) {
                _uiState.update {
                    it.copy(
                        confirmando = false,
                        avisoConfirmar = "No se ha encontrado la parcela del borrador en " +
                            "tu lista de parcelas. Elígela antes de confirmar."
                    )
                }
            } else {
                val cuerpo = JSONObject()
                    .put("fecha", borrador.optString("fecha"))
                    .put("parcela_id", parcela.id)
                    .put("categoria", borrador.optString("categoria"))
                    .put("campos", borrador.optJSONObject("campos") ?: JSONObject())
                    .put("observaciones", borrador.optString("observaciones"))
                    // Una Nota de voz produce como máximo una Labor: el id de la
                    // nota hace la creación idempotente en el backend.
                    .put("nota_id", notaId)
                laboresRepo.guardarLabor(token, cuerpo) { campana, texto ->
                    _uiState.update { it.copy(confirmando = false) }
                    if (campana != null) {
                        alConfirmar()
                    } else {
                        _uiState.update { it.copy(avisoConfirmar = texto) }
                    }
                }
            }
        }
    }

    fun sinPermiso() {
        _uiState.update {
            it.copy(aviso = "Sin permiso de micrófono no se puede responder por voz.")
        }
    }

    private fun aFicha(json: String): JSONObject? = try {
        JSONObject(json)
    } catch (e: Exception) {
        null
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
    alDescartar: () -> Unit,
    viewModel: BorradorViewModel = hiltViewModel(key = "borrador-$notaId")
) {
    val contexto = LocalContext.current
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.preparar(notaId, json)
    }

    val borrador = try {
        JSONObject(estado.fichaJson)
    } catch (e: Exception) {
        null
    }

    // Confirmación antes del descarte (no hay papelera que lo recupere).
    if (estado.preguntarDescarte) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarDescartar,
            title = {
                Text(
                    text = "¿Descartar este borrador?",
                    style = TextoValor,
                    color = Texto
                )
            },
            text = {
                Text(
                    text = "La nota quedará descartada y no se creará ninguna labor. " +
                        "No se puede deshacer.",
                    style = TextoCuerpo,
                    color = Texto
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarDescartar(alDescartar) }) {
                    Text(text = "Descartar", style = TextoBoton, color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarDescartar) {
                    Text(text = "Cancelar", style = TextoBoton, color = Primario)
                }
            }
        )
    }

    if (borrador == null) return
    if (borrador.optBoolean("rechazado")) {
        Text(
            text = "No se ha reconocido la categoría de la labor. Solo hay tres: " +
                "tratamiento fitosanitario, riego y cosecha. Vuelve a dictar la nota.",
            style = TextoCuerpo,
            color = Error
        )
        Spacer(modifier = Modifier.height(8.dp))
        BotonPeligro(texto = "Descartar", icono = R.drawable.ic_eliminar, alPulsar = viewModel::pedirDescartar)
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
    val fechaResuelta = fechaIsoDeBorrador(borrador)
    val campanaDeLaFecha = if (fechaResuelta.isEmpty()) {
        null
    } else {
        estado.campanas.firstOrNull { it.id == idCampanaDeLaFecha(fechaResuelta) }
    }
    val campanaActual = estado.campanas.firstOrNull { it.estado == "actual" }
    val fechaAdmitida = fechaResuelta.isEmpty() || estado.campanas.isEmpty() ||
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

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            viewModel.empezarRespuesta(alGuardar)
        } else {
            viewModel.sinPermiso()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Primario, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Borrador IA",
            style = TextoValor,
            color = Primario
        )
        Spacer(modifier = Modifier.height(8.dp))
        for ((etiqueta, valor) in lineasDeBorrador(borrador)) {
            Text(
                text = "$etiqueta: $valor",
                style = TextoCuerpo,
                color = Texto
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (!parcelaResuelta) {
            // feature borrador-sin-parcela: la IA no identificó la parcela y el
            // socio elige una de su lista antes de poder confirmar.
            Text(
                text = "Elige la parcela de esta labor:",
                style = TextoCuerpo,
                fontWeight = FontWeight.Bold,
                color = Error
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (estado.cargandoParcelas) {
                Text(
                    text = "Cargando tus parcelas…",
                    style = TextoCuerpo,
                    color = Texto
                )
            } else if (estado.parcelas.isEmpty()) {
                Text(
                    text = "Todavía no has creado ninguna parcela. Crea una parcela " +
                        "para poder apuntar esta labor.",
                    style = TextoCuerpo,
                    color = Error
                )
            } else {
                Desplegable(
                    etiqueta = "Parcela",
                    valor = "",
                    opciones = estado.parcelas,
                    alElegir = { elegida ->
                        // La parcela elegida queda guardada en la ficha.
                        viewModel.elegirParcela(elegida, alGuardar)
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
                style = TextoCuerpo,
                fontWeight = FontWeight.Bold,
                color = Error
            )
            Spacer(modifier = Modifier.height(8.dp))
            BotonPrincipal(texto = "Elegir fecha", alPulsar = viewModel::abrirCalendario)
            Spacer(modifier = Modifier.height(12.dp))
        } else if (!fechaAdmitida) {
            Text(
                text = avisoDeFecha,
                style = TextoCuerpo,
                fontWeight = FontWeight.Bold,
                color = Error
            )
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(texto = "Cambiar la fecha", alPulsar = viewModel::abrirCalendario)
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (estado.eligiendoFecha) {
            CalendarioDeFecha(
                alElegir = { elegida -> viewModel.elegirFecha(elegida, alGuardar) },
                alCerrar = viewModel::cerrarCalendario
            )
        }
        val completo = faltantes.isEmpty() && parcelaResuelta &&
            fechaResuelta.isNotEmpty() && fechaAdmitida
        if (completo) {
            Text(
                text = "Borrador completo. Ya se puede confirmar.",
                style = TextoCuerpo,
                color = Primario
            )
            Spacer(modifier = Modifier.height(8.dp))
            BotonPrincipal(
                texto = if (estado.confirmando) "Confirmando…" else "Confirmar",
                icono = R.drawable.ic_confirmada,
                habilitado = !estado.grabando && !estado.enviando && !estado.confirmando,
                alPulsar = { viewModel.confirmar(alConfirmar) }
            )
            estado.avisoConfirmar?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = TextoCuerpo,
                    color = Error
                )
            }
        } else {
            if (faltantes.isNotEmpty()) {
                Text(
                    text = "Faltan datos por responder: " +
                        faltantes.joinToString(", ") { etiquetaDeCampo(it) } + ".",
                    style = TextoCuerpo,
                    fontWeight = FontWeight.Bold,
                    color = Error
                )
                val pregunta = borrador.optString("pregunta")
                if (pregunta.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "El asistente te pregunta:",
                        style = TextoCuerpo,
                        fontWeight = FontWeight.Bold,
                        color = Primario
                    )
                    Text(
                        text = pregunta,
                        style = TextoCuerpo,
                        color = Texto
                    )
                }
                estado.ultimaRespuesta?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tu respuesta: $it",
                        style = TextoCuerpo,
                        color = Texto
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (estado.grabando) {
                    Text(
                        text = "Grabando tu respuesta… ${estado.segundos} de " +
                            "$LIMITE_SEGUNDOS_RESPUESTA segundos",
                        style = TextoCuerpo,
                        color = Error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BotonPeligro(
                        texto = "Parar",
                        alPulsar = { viewModel.pararRespuesta(alGuardar) }
                    )
                } else if (estado.enviando) {
                    Text(
                        text = "Enviando tu respuesta…",
                        style = TextoCuerpo,
                        color = Primario
                    )
                } else if (estado.hayRespuestaSinEnviar) {
                    // Respuesta ya grabada que aún no se ha podido enviar.
                    BotonPrincipal(
                        texto = "Reintentar",
                        icono = R.drawable.ic_refrescar,
                        alPulsar = { viewModel.enviarRespuesta(alGuardar) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BotonSecundario(
                        texto = "Responder otra vez",
                        icono = R.drawable.ic_mic,
                        alPulsar = { viewModel.reiniciarRespuesta(alGuardar) }
                    )
                } else {
                    BotonPrincipal(
                        texto = "Responder por voz",
                        icono = R.drawable.ic_mic,
                        alPulsar = {
                            if (
                                ContextCompat.checkSelfPermission(
                                    contexto, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.empezarRespuesta(alGuardar)
                            } else {
                                lanzadorPermiso.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                }
                estado.aviso?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = TextoCuerpo,
                        color = Error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Mientras falte un campo obligatorio o la parcela, el borrador no
            // se puede confirmar.
            BotonPrincipal(texto = "Confirmar", icono = R.drawable.ic_confirmada, habilitado = false, alPulsar = {})
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
                style = TextoCuerpo,
                color = Texto
            )
        }
        // El socio puede rechazar el borrador en cualquier momento, con la
        // ficha completa o incompleta: la nota queda descartada y no se crea
        // ninguna labor.
        Spacer(modifier = Modifier.height(12.dp))
        BotonPeligro(texto = "Descartar", icono = R.drawable.ic_eliminar, alPulsar = viewModel::pedirDescartar)
    }
}

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
                Text(text = "Aceptar", style = TextoBoton, color = Primario)
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) {
                Text(text = "Cancelar", style = TextoBoton, color = Primario)
            }
        }
    ) {
        DatePicker(state = estado)
    }
}
