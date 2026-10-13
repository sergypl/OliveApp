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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.delay
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Dirección visual de DESIGN.md: fondo crema, texto marrón oscuro, cuerpo 18sp mínimo.
private val FondoCrema = Color(0xFFFAF7F0)
private val TextoMarron = Color(0xFF2E2A20)
private val VerdeOliva = Color(0xFF5A6E3A)
private val TextoError = Color(0xFF8B1E1E)

// Backend en desarrollo: cada dispositivo (emulador o móvil físico) alcanza el
// Mac del host por su propio localhost gracias a `adb reverse tcp:8000 tcp:8000`
// (init.sh lo configura en cada dispositivo conectado).
private const val URL_BASE = "http://127.0.0.1:8000"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OlivApp() }
    }
}

@Composable
fun OlivApp() {
    val contexto = LocalContext.current
    // Si ya hay token guardado, la sesión sigue abierta tras reiniciar la app.
    var pantalla by remember {
        mutableStateOf(if (AlmacenSesiones(contexto).hayToken()) "sesion" else "inicio")
    }
    var idCampana by remember { mutableStateOf<String?>(null) }
    var laborEnEdicion by remember { mutableStateOf<LaborEditable?>(null) }
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
            onAbrirCampana = { id ->
                idCampana = id
                pantalla = "campana"
            }
        )
        "parcelas" -> PantallaParcelas(onVolver = { pantalla = "sesion" })
        "notas_voz" -> PantallaNotasVoz(onVolver = { pantalla = "sesion" })
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

@Composable
fun PantallaBienvenida(onIniciarSesion: () -> Unit, onCrearCuenta: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCrema)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OlivApp",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tus campañas y tu cuaderno de campo,\na la mano.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        BotonPrincipal(texto = "Iniciar sesión", alPulsar = onIniciarSesion)
        Spacer(modifier = Modifier.height(16.dp))
        BotonSecundario(texto = "Crear cuenta", alPulsar = onCrearCuenta)
    }
}

@Composable
fun PantallaRegistro(onVolver: () -> Unit, onIniciarSesion: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var codigoSocio by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var registrada by remember { mutableStateOf(false) }
    var trabajando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Crear cuenta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Con tu email, una contraseña y tu código de socio.",
            fontSize = 18.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CampoDeTexto(
            etiqueta = "Email",
            valor = email,
            alCambiar = { email = it },
            tipo = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Contraseña",
            valor = contrasena,
            alCambiar = { contrasena = it },
            tipo = KeyboardType.Password,
            oculto = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mínimo 8 caracteres.",
            fontSize = 16.sp,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Código de socio",
            valor = codigoSocio,
            alCambiar = { codigoSocio = it },
            tipo = KeyboardType.Text
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "El que te ha dado tu cooperativa (p. ej. Socio 1).",
            fontSize = 16.sp,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(
            texto = if (trabajando) "Creando la cuenta…" else "Crear cuenta",
            habilitado = !trabajando,
            alPulsar = {
                trabajando = true
                mensaje = null
                registrada = false
                ServicioCuentas.crearCuenta(email, contrasena, codigoSocio) { exitosa, texto ->
                    trabajando = false
                    registrada = exitosa
                    mensaje = texto
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        mensaje?.let {
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = if (registrada) VerdeOliva else TextoError,
                textAlign = TextAlign.Center
            )
        }
        if (registrada) {
            Spacer(modifier = Modifier.height(24.dp))
            BotonSecundario(texto = "Ir a iniciar sesión", alPulsar = onIniciarSesion)
        }
        Spacer(modifier = Modifier.height(24.dp))
        BotonSecundario(texto = "Volver al inicio", alPulsar = onVolver)
    }
}

@Composable
fun PantallaInicioSesion(
    onVolver: () -> Unit,
    onCrearCuenta: () -> Unit,
    onSesionAbierta: () -> Unit
) {
    val contexto = LocalContext.current
    var email by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var trabajando by remember { mutableStateOf(false) }

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
            valor = email,
            alCambiar = { email = it },
            tipo = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Contraseña",
            valor = contrasena,
            alCambiar = { contrasena = it },
            tipo = KeyboardType.Password,
            oculto = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(
            texto = if (trabajando) "Entrando…" else "Iniciar sesión",
            habilitado = !trabajando,
            alPulsar = {
                trabajando = true
                mensaje = null
                ServicioCuentas.iniciarSesion(email, contrasena) { token, texto ->
                    trabajando = false
                    if (token != null) {
                        AlmacenSesiones(contexto).guardar(token)
                        onSesionAbierta()
                    } else {
                        mensaje = texto
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        mensaje?.let {
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

/**
 * Panel de la campaña actual (pantalla de aterrizaje tras entrar).
 *
 * Muestra los datos oficiales de la cooperativa en su propio bloque,
 * separados de las labores (que vivirán en el cuaderno de campo), y el
 * acceso al cuaderno y al simulador de liquidación.
 */
@Composable
fun PantallaPanel(
    onSesionCerrada: () -> Unit,
    onAbrirParcelas: () -> Unit,
    onAbrirCuaderno: () -> Unit,
    onAbrirNotasVoz: () -> Unit,
    onAbrirCampana: (String) -> Unit
) {
    val contexto = LocalContext.current
    var datos by remember { mutableStateOf<DatosPanel?>(null) }
    var campanas by remember { mutableStateOf<List<DatosCampanaResumen>>(emptyList()) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }

    // Llamadas autenticadas: el token guardado acompaña a las peticiones.
    remember {
        val token = AlmacenSesiones(contexto).token()
        if (token == null) {
            mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
        } else {
            ServicioCuentas.cargarMiPanel(token) { cargados, texto ->
                datos = cargados
                mensaje = texto
            }
            ServicioCuentas.cargarCampanas(token) { lista, texto ->
                if (lista != null) {
                    campanas = lista
                } else {
                    mensaje = texto
                }
            }
        }
        true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        datos?.let {
            Text(
                text = "Campaña ${it.campana}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextoMarron
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Datos oficiales de la cooperativa",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                FilaDato(etiqueta = "Kilos entregados", valor = it.kilosEntregados)
                Spacer(modifier = Modifier.height(12.dp))
                FilaDato(etiqueta = "Rendimiento graso", valor = it.rendimientoGraso)
                Spacer(modifier = Modifier.height(12.dp))
                FilaDato(etiqueta = "Aceite obtenido", valor = it.aceiteObtenido)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Estos son los datos que declara la cooperativa.\nTus labores van aparte, en el cuaderno de campo.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron,
                textAlign = TextAlign.Center
            )
        } ?: Text(
            text = "Cargando tus datos…",
            fontSize = 18.sp,
            color = TextoMarron
        )
        mensaje?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoError,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        val cerradas = campanas.filter { it.estado == "cerrada" }
        if (cerradas.isNotEmpty()) {
            Text(
                text = "Campañas anteriores",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(12.dp))
            for (c in cerradas) {
                BotonSecundario(
                    texto = "Campaña ${c.nombre}",
                    alPulsar = { onAbrirCampana(c.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(
            text = "Tus herramientas",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeOliva
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Botón grande de micrófono del panel: grabar una nota de voz.
        Button(
            onClick = onAbrirNotasVoz,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VerdeOliva,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Dictar una nota de voz",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Graba una nota corta para apuntar una labor.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        BotonSecundario(texto = "Tus parcelas", alPulsar = onAbrirParcelas)
        Spacer(modifier = Modifier.height(12.dp))
        BotonSecundario(texto = "Cuaderno de campo", alPulsar = onAbrirCuaderno)
        Spacer(modifier = Modifier.height(12.dp))
        BotonSecundario(
            texto = "Simulador de liquidación",
            alPulsar = { aviso = "El simulador de liquidación estará disponible próximamente." }
        )
        aviso?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        BotonSecundario(
            texto = "Cerrar sesión",
            alPulsar = {
                // Se borra el token del dispositivo: sin él no hay acceso.
                AlmacenSesiones(contexto).borrar()
                onSesionCerrada()
            }
        )
    }
}

/**
 * Pantalla "Mi cuaderno" (feature consulta-cuaderno-por-campana).
 *
 * Historial cronológico de las labores del socio agrupado por campaña: de la
 * campaña más reciente a la más antigua y, dentro de cada campaña, de la
 * labor más reciente a la más antigua, con los campos tipo SIEX de cada una
 * legibles de un vistazo. Solo labores vigentes (sin papelera).
 */
@Composable
fun PantallaCuaderno(
    onVolver: () -> Unit,
    onApuntarLabor: () -> Unit,
    onEditarLabor: (LaborEditable) -> Unit
) {
    val contexto = LocalContext.current
    var campanas by remember { mutableStateOf<List<DatosCampanaDeLabores>>(emptyList()) }
    var cargado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var laborParaEliminar by remember { mutableStateOf<LaborEditable?>(null) }

    fun cargar() {
        val token = AlmacenSesiones(contexto).token() ?: return
        ServicioCuentas.cargarMisLabores(token) { lista, texto ->
            cargado = true
            if (lista != null) {
                campanas = lista
            } else {
                mensaje = texto
            }
        }
    }

    remember {
        val token = AlmacenSesiones(contexto).token()
        if (token == null) {
            mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
            cargado = true
        } else {
            cargar()
        }
        true
    }

    // Confirmación antes del borrado directo (no hay papelera que lo recupere).
    laborParaEliminar?.let { labor ->
        AlertDialog(
            onDismissRequest = { laborParaEliminar = null },
            title = {
                Text(
                    text = "¿Eliminar esta labor?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoMarron
                )
            },
            text = {
                Text(
                    text = labor.fechaVisible + " · " +
                        (NOMBRES_DE_CATEGORIA[labor.categoria] ?: labor.categoria) +
                        "\n\nSe borrará de tu cuaderno. No se puede deshacer.",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoMarron
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val objetivo = labor
                    laborParaEliminar = null
                    val token = AlmacenSesiones(contexto).token()
                    if (token == null) {
                        mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
                    } else {
                        ServicioCuentas.eliminarLabor(token, objetivo.id) { exito, texto ->
                            mensaje = texto
                            if (exito) {
                                cargar()
                            }
                        }
                    }
                }) {
                    Text(text = "Eliminar", fontSize = 20.sp, color = TextoError)
                }
            },
            dismissButton = {
                TextButton(onClick = { laborParaEliminar = null }) {
                    Text(text = "Cancelar", fontSize = 20.sp, color = VerdeOliva)
                }
            }
        )
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
            text = "Mi cuaderno",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Historial de tus labores por campaña,\nde la más reciente a la más antigua.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(texto = "Apuntar una labor", alPulsar = onApuntarLabor)
        Spacer(modifier = Modifier.height(24.dp))
        if (!cargado) {
            Text(
                text = "Cargando tu cuaderno…",
                fontSize = 18.sp,
                color = TextoMarron
            )
        } else if (campanas.isEmpty() && mensaje == null) {
            Text(
                text = "Todavía no has apuntado labores.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron,
                textAlign = TextAlign.Center
            )
        } else {
            for ((indice, campana) in campanas.withIndex()) {
                if (indice > 0) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Campaña " + campana.nombre +
                            if (campana.esActual) " · Campaña actual" else "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeOliva
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        for ((indiceLabor, labor) in campana.labores.withIndex()) {
                            if (indiceLabor > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            FilaLabor(
                                labor,
                                alEditar = { onEditarLabor(labor.editable) },
                                alEliminar = { laborParaEliminar = labor.editable }
                            )
                        }
                    }
                }
            }
        }
        mensaje?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoError,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        BotonSecundario(texto = "Volver al panel", alPulsar = onVolver)
    }
}

/**
 * Formulario de una labor de "Mi cuaderno" (features alta-manual-labor y
 * edicion-de-labor): apunta una labor nueva o edita una existente.
 *
 * Campos por categoría según ADR 0005. La labor se guarda en la campaña
 * que corresponde a su fecha (campaña oleícola del 1 de noviembre al
 * 31 de octubre).
 */
@Composable
fun PantallaNuevaLabor(
    onVolver: () -> Unit,
    onVerCampana: (String) -> Unit,
    labor: LaborEditable? = null
) {
    val contexto = LocalContext.current
    var fecha by remember { mutableStateOf(labor?.fechaVisible ?: fechaDeHoy()) }
    var parcelas by remember { mutableStateOf<List<DatosParcela>>(emptyList()) }
    var parcelaId by remember { mutableStateOf(labor?.parcelaId ?: "") }
    var parcelaNombre by remember { mutableStateOf(labor?.parcelaNombre ?: "") }
    var categoria by remember { mutableStateOf(labor?.categoria ?: "") }
    var producto by remember { mutableStateOf(labor?.campos?.optString("producto") ?: "") }
    var plaga by remember { mutableStateOf(labor?.campos?.optString("plaga_a_controlar") ?: "") }
    var cantidad by remember { mutableStateOf(numeroInicial(labor, "cantidad")) }
    var unidad by remember { mutableStateOf(labor?.campos?.optString("unidad") ?: "") }
    var modalidad by remember { mutableStateOf(labor?.campos?.optString("modalidad") ?: "") }
    var kilos by remember { mutableStateOf(numeroInicial(labor, "kilos")) }
    var observaciones by remember { mutableStateOf(labor?.observaciones ?: "") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var guardada by remember { mutableStateOf(false) }
    var campanaGuardada by remember { mutableStateOf<String?>(null) }
    var trabajando by remember { mutableStateOf(false) }

    remember {
        val token = AlmacenSesiones(contexto).token()
        if (token == null) {
            mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
        } else {
            ServicioCuentas.cargarParcelas(token) { lista, texto ->
                if (lista != null) {
                    parcelas = lista
                } else {
                    mensaje = texto
                }
            }
        }
        true
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
            text = if (labor == null) "Apuntar una labor" else "Editar labor",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (labor == null) {
                "Va a Mi cuaderno, en la campaña de su fecha."
            } else {
                "Los cambios se reflejan en Mi cuaderno."
            },
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CampoDeTexto(
            etiqueta = "Fecha (día/mes/año)",
            valor = fecha,
            alCambiar = { fecha = it },
            tipo = KeyboardType.Text
        )
        Spacer(modifier = Modifier.height(16.dp))
        Desplegable(
            etiqueta = "Parcela",
            valor = parcelaNombre,
            opciones = parcelas.map { it.nombre },
            alElegir = { nombre ->
                parcelaNombre = nombre
                parcelaId = parcelas.firstOrNull { it.nombre == nombre }?.id ?: ""
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¿Qué labor es?",
            fontSize = 18.sp,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(8.dp))
        OpcionCategoria(
            texto = "Tratamiento fitosanitario",
            elegida = categoria == "tratamiento",
            alPulsar = {
                categoria = "tratamiento"
                unidad = ""
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OpcionCategoria(
            texto = "Riego",
            elegida = categoria == "riego",
            alPulsar = {
                categoria = "riego"
                unidad = ""
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OpcionCategoria(
            texto = "Cosecha",
            elegida = categoria == "cosecha",
            alPulsar = { categoria = "cosecha" }
        )
        Spacer(modifier = Modifier.height(16.dp))
        when (categoria) {
            "tratamiento" -> {
                CampoDeTexto(
                    etiqueta = "Producto",
                    valor = producto,
                    alCambiar = { producto = it },
                    tipo = KeyboardType.Text
                )
                Spacer(modifier = Modifier.height(16.dp))
                Desplegable(
                    etiqueta = "Plaga a controlar",
                    valor = plaga,
                    opciones = listOf(
                        "mosca del olivo", "repilo", "prays", "tuberculosis",
                        "cochinilla", "malas hierbas", "otra"
                    ),
                    alElegir = { plaga = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                CampoDeTexto(
                    etiqueta = "Cantidad",
                    valor = cantidad,
                    alCambiar = { cantidad = it },
                    tipo = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(16.dp))
                Desplegable(
                    etiqueta = "Unidad",
                    valor = unidad,
                    opciones = listOf("kg", "L", "g", "ml"),
                    alElegir = { unidad = it }
                )
            }
            "riego" -> {
                CampoDeTexto(
                    etiqueta = "Cantidad",
                    valor = cantidad,
                    alCambiar = { cantidad = it },
                    tipo = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(16.dp))
                Desplegable(
                    etiqueta = "Unidad",
                    valor = unidad,
                    opciones = listOf("horas", "m³", "L"),
                    alElegir = { unidad = it }
                )
            }
            "cosecha" -> {
                Desplegable(
                    etiqueta = "Modalidad",
                    valor = modalidad,
                    opciones = listOf("a vuelo", "a suelo"),
                    alElegir = { modalidad = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                CampoDeTexto(
                    etiqueta = "Kilos",
                    valor = kilos,
                    alCambiar = { kilos = it },
                    tipo = KeyboardType.Decimal
                )
            }
        }
        if (categoria.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            CampoDeTexto(
                etiqueta = "Observaciones (opcional)",
                valor = observaciones,
                alCambiar = { observaciones = it },
                tipo = KeyboardType.Text
            )
            Spacer(modifier = Modifier.height(24.dp))
            BotonPrincipal(
                texto = if (trabajando) {
                    if (labor == null) "Guardando la labor…" else "Guardando los cambios…"
                } else {
                    if (labor == null) "Guardar labor" else "Guardar cambios"
                },
                habilitado = !trabajando,
                alPulsar = {
                    trabajando = true
                    mensaje = null
                    guardada = false
                    campanaGuardada = null
                    val token = AlmacenSesiones(contexto).token()
                    val iso = fechaIso(fecha)
                    if (token == null) {
                        trabajando = false
                        mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
                    } else if (iso == null) {
                        trabajando = false
                        mensaje = "Escribe la fecha como día, mes y año (por ejemplo, 15/12/2025)."
                    } else {
                        val campos = JSONObject()
                        when (categoria) {
                            "tratamiento" -> campos
                                .put("producto", producto)
                                .put("plaga_a_controlar", plaga)
                                .put("cantidad", cantidad.replace(',', '.'))
                                .put("unidad", unidad)
                            "riego" -> campos
                                .put("cantidad", cantidad.replace(',', '.'))
                                .put("unidad", unidad)
                            "cosecha" -> campos
                                .put("modalidad", modalidad)
                                .put("kilos", kilos.replace(',', '.'))
                        }
                        val cuerpo = JSONObject()
                            .put("fecha", iso)
                            .put("parcela_id", parcelaId)
                            .put("categoria", categoria)
                            .put("campos", campos)
                            .put("observaciones", observaciones)
                        val laborEditada = labor
                        if (laborEditada != null) {
                            ServicioCuentas.actualizarLabor(
                                token, laborEditada.id, cuerpo
                            ) { exito, texto ->
                                trabajando = false
                                guardada = exito
                                mensaje = texto
                            }
                        } else {
                            ServicioCuentas.guardarLabor(token, cuerpo) { campana, texto ->
                                trabajando = false
                                guardada = campana != null
                                campanaGuardada = campana
                                mensaje = texto
                            }
                        }
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        mensaje?.let {
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = if (guardada) VerdeOliva else TextoError,
                textAlign = TextAlign.Center
            )
        }
        campanaGuardada?.let {
            Spacer(modifier = Modifier.height(24.dp))
            BotonSecundario(
                texto = "Ver Mi cuaderno de esa campaña",
                alPulsar = { onVerCampana(it) }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        BotonSecundario(texto = "Volver a Mi cuaderno", alPulsar = onVolver)
    }
}

/** Fecha de hoy en formato español (DD/MM/AAAA). */
private fun fechaDeHoy(): String {
    val hoy = java.time.LocalDate.now()
    return "%02d/%02d/%04d".format(hoy.dayOfMonth, hoy.monthValue, hoy.year)
}

/** Fecha en formato español (DD/MM/AAAA) a ISO (AAAA-MM-DD), o null si no vale. */
private fun fechaIso(texto: String): String? {
    val partes = texto.trim().split("/")
    if (partes.size != 3) return null
    val dia = partes[0].toIntOrNull() ?: return null
    val mes = partes[1].toIntOrNull() ?: return null
    val anio = partes[2].toIntOrNull() ?: return null
    return try {
        java.time.LocalDate.of(anio, mes, dia).toString()
    } catch (e: Exception) {
        null
    }
}

/** Botón de categoría: relleno verde cuando está elegida. */
@Composable
fun OpcionCategoria(texto: String, elegida: Boolean, alPulsar: () -> Unit) {
    if (elegida) {
        Button(
            onClick = alPulsar,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VerdeOliva,
                contentColor = Color.White
            )
        ) {
            Text(text = texto, fontSize = 18.sp)
        }
    } else {
        OutlinedButton(
            onClick = alPulsar,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = texto, fontSize = 18.sp, color = VerdeOliva)
        }
    }
}

/** Lista desplegable grande y clara (parcela, plaga, unidad, modalidad). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Desplegable(
    etiqueta: String,
    valor: String,
    opciones: List<String>,
    alElegir: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta, fontSize = 18.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                color = TextoMarron
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            for (opcion in opciones) {
                DropdownMenuItem(
                    text = { Text(opcion, fontSize = 18.sp) },
                    onClick = {
                        alElegir(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

/**
 * Pantalla de notas de voz (features grabacion-de-nota-voz,
 * transcripcion-de-nota-voz y cola-offline-de-notas).
 *
 * El socio graba una nota corta con el botón grande; la grabación queda
 * registrada como nota de voz con estado visible y se envía al backend para
 * transcribirla (el servidor no conserva el audio). Sin conexión, la nota
 * queda en la cola local (Room) en estado 'pendiente' con aviso visible y
 * puede reintentarse desde la cola.
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
            ServicioCuentas.transcribirNota(token, File(nota.fichero)) { texto, error, sinConexion ->
                if (texto != null) {
                    almacen.marcar(nota.id, "transcrita", texto)
                    mensaje = "Nota transcrita. Ya puedes leer el texto."
                } else if (sinConexion) {
                    // Sin conexión la nota no se pierde: vuelve a la cola local
                    // en estado 'pendiente' (feature cola-offline-de-notas).
                    almacen.marcar(nota.id, "pendiente", "")
                    mensaje =
                        "Sin conexión con el servidor. Tu nota está guardada en el teléfono, pendiente de enviar."
                } else {
                    almacen.marcar(nota.id, "error", "")
                    mensaje = error
                }
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

    // Al abrir la pantalla, las notas pendientes se envían a transcribir.
    LaunchedEffect(Unit) {
        val token = AlmacenSesiones(contexto).token()
        for (nota in almacen.notas()) {
            if (nota.estado == "pendiente" && token != null) {
                transcribirNota(nota)
            }
        }
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
                        FilaNotaVoz(nota, alReintentar = { transcribirNota(nota) })
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
                        FilaNotaVoz(nota, alReintentar = { transcribirNota(nota) })
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
fun FilaNotaVoz(nota: AlmacenNotasVoz.NotaDeVoz, alReintentar: () -> Unit) {
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
            color = if (nota.estado == "error") TextoError else VerdeOliva
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
        if (nota.estado == "error" || nota.estado == "pendiente") {
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(texto = "Reintentar", alPulsar = alReintentar)
        }
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
@Entity(tableName = "notas_voz")
data class NotaVozFila(
    @PrimaryKey val id: String,
    val fichero: String,
    @ColumnInfo(name = "creada_en") val creadaEn: String,
    val segundos: Int,
    val estado: String,
    val texto: String
)

/** Acceso a la cola de notas de voz en Room. */
@Dao
interface NotasVozDao {
    @Query("SELECT * FROM notas_voz ORDER BY creada_en DESC")
    fun notas(): List<NotaVozFila>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertar(nota: NotaVozFila)

    @Query("UPDATE notas_voz SET estado = :estado, texto = :texto WHERE id = :id")
    fun marcar(id: String, estado: String, texto: String)
}

/** Base Room de la cola de notas de voz. */
@Database(entities = [NotaVozFila::class], version = 1, exportSchema = false)
abstract class BaseNotasVoz : RoomDatabase() {
    abstract fun dao(): NotasVozDao

    companion object {
        @Volatile
        private var instancia: BaseNotasVoz? = null

        fun de(contexto: Context): BaseNotasVoz =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    BaseNotasVoz::class.java,
                    "notas-voz.db"
                )
                    // La cola es pequeña y las consultas son de pocos milisegundos:
                    // el almacén mantiene la interfaz síncrona del resto de la app.
                    .allowMainThreadQueries()
                    .build()
                    .also { instancia = it }
            }
    }
}

class AlmacenNotasVoz(private val contexto: Context) {

    class NotaDeVoz(
        val id: String,
        val fichero: String,
        val cuando: String,
        val segundos: Int,
        val estado: String,
        val texto: String
    ) {
        val estadoVisible: String
            get() = when (estado) {
                "pendiente" -> "Pendiente de enviar. Está en la cola del teléfono."
                "transcribiendo" -> "Transcribiendo…"
                "transcrita" -> "Transcrita"
                "error" -> "No se ha podido transcribir. Toca para reintentar."
                else -> "Pendiente de transcripción"
            }
    }

    private val carpeta = File(contexto.filesDir, "notas-voz")
    private val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val dao = BaseNotasVoz.de(contexto).dao()

    init {
        importarIndiceAntiguo()
    }

    fun nuevaRutaDeAudio(): File {
        carpeta.mkdirs()
        return File(carpeta, "nota-" + System.currentTimeMillis() + ".m4a")
    }

    /** Registra una nota grabada en la cola (la más reciente la primera). */
    fun guardar(fichero: File, segundos: Int): NotaDeVoz {
        val ahora = LocalDateTime.now()
        dao.insertar(
            NotaVozFila(
                id = fichero.name,
                fichero = fichero.absolutePath,
                creadaEn = ahora.toString(),
                segundos = segundos,
                estado = "pendiente",
                texto = ""
            )
        )
        return NotaDeVoz(
            id = fichero.name,
            fichero = fichero.absolutePath,
            cuando = ahora.format(formato),
            segundos = segundos,
            estado = "pendiente",
            texto = ""
        )
    }

    /** Cambia el estado de una nota (y guarda su transcripción cuando llega). */
    fun marcar(id: String, estado: String, texto: String) {
        dao.marcar(id, estado, texto)
    }

    fun notas(): List<NotaDeVoz> =
        dao.notas().map { fila ->
            val creada = try {
                LocalDateTime.parse(fila.creadaEn).format(formato)
            } catch (e: Exception) {
                fila.creadaEn
            }
            NotaDeVoz(
                id = fila.id,
                fichero = fila.fichero,
                cuando = creada,
                segundos = fila.segundos,
                estado = fila.estado,
                texto = fila.texto
            )
        }

    /**
     * Migración única desde el índice JSON de las versiones anteriores (antes
     * de cola-offline-de-notas): lo que haya ahí pasa a Room y se borra el
     * índice, para que ninguna nota grabada se pierda en el cambio.
     */
    private fun importarIndiceAntiguo() {
        val indice = File(contexto.filesDir, "notas-voz.json")
        if (!indice.exists()) return
        try {
            val filas = JSONArray(indice.readText())
            for (i in 0 until filas.length()) {
                val fila = filas.optJSONObject(i) ?: continue
                dao.insertar(
                    NotaVozFila(
                        id = fila.optString("id"),
                        fichero = fila.optString("fichero"),
                        creadaEn = fila.optString("creada_en"),
                        segundos = fila.optInt("segundos"),
                        estado = fila.optString("estado", "pendiente"),
                        texto = fila.optString("texto")
                    )
                )
            }
            indice.delete()
        } catch (e: Exception) {
            // El índice antiguo no se pudo leer: la cola de Room sigue mandando.
        }
    }
}

/** Grabadora de una nota de voz: MPEG-4 con audio AAC. */
class GrabadorNotas {
    private var grabador: MediaRecorder? = null
    private var empezadoEn = 0L

    @Suppress("DEPRECATION")
    fun empezar(fichero: File) {
        val r = MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(16000)
        r.setOutputFile(fichero.absolutePath)
        r.prepare()
        r.start()
        grabador = r
        empezadoEn = android.os.SystemClock.elapsedRealtime()
    }

    /** Para la grabación y devuelve los segundos grabados. */
    fun parar(): Int {
        val r = grabador ?: return 0
        grabador = null
        val segundos = ((android.os.SystemClock.elapsedRealtime() - empezadoEn) / 1000).toInt()
        try {
            r.stop()
        } catch (e: Exception) {
            // Grabación demasiado corta: stop() falla y el fichero no sirve.
        }
        r.release()
        return segundos
    }
}

/**
 * Pantalla de parcelas del socio: alta con nombre propio y lista.
 *
 * Las parcelas no se eliminan ni archivan en el MVP (docs/domain-model.md).
 */
@Composable
fun PantallaParcelas(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var parcelas by remember { mutableStateOf<List<DatosParcela>>(emptyList()) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var creada by remember { mutableStateOf(false) }
    var trabajando by remember { mutableStateOf(false) }

    fun cargar() {
        val token = AlmacenSesiones(contexto).token() ?: return
        ServicioCuentas.cargarParcelas(token) { lista, texto ->
            if (lista != null) {
                parcelas = lista
            } else {
                mensaje = texto
            }
        }
    }

    remember {
        val token = AlmacenSesiones(contexto).token()
        if (token == null) {
            mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
        } else {
            cargar()
        }
        true
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
            text = "Tus parcelas",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aquí viven tus parcelas.\nLes pondrás labores desde el cuaderno.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CampoDeTexto(
            etiqueta = "Nombre de la parcela",
            valor = nombre,
            alCambiar = { nombre = it },
            tipo = KeyboardType.Text
        )
        Spacer(modifier = Modifier.height(16.dp))
        BotonPrincipal(
            texto = if (trabajando) "Creando la parcela…" else "Crear parcela",
            habilitado = !trabajando,
            alPulsar = {
                trabajando = true
                mensaje = null
                creada = false
                val token = AlmacenSesiones(contexto).token()
                if (token == null) {
                    trabajando = false
                    mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
                } else {
                    ServicioCuentas.crearParcela(token, nombre) { exitosa, texto ->
                        trabajando = false
                        creada = exitosa
                        mensaje = texto
                        if (exitosa) {
                            nombre = ""
                            cargar()
                        }
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        mensaje?.let {
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = if (creada) VerdeOliva else TextoError,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (parcelas.isNotEmpty()) {
            Text(
                text = "Parcelas de este socio",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                for ((indice, parcela) in parcelas.withIndex()) {
                    if (indice > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        text = parcela.nombre,
                        fontSize = 18.sp,
                        color = TextoMarron
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = "Las parcelas no se borran en esta versión.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonSecundario(texto = "Volver al panel", alPulsar = onVolver)
    }
}

/**
 * Pantalla de una campaña anterior (modo solo consulta).
 *
 * Muestra los datos oficiales de la cooperativa y las labores de "Mi
 * cuaderno" por separado. Al estar cerrada la campaña, no se pueden añadir
 * labores nuevas (estado `cerrada` de Campaña en docs/domain-model.md).
 */
@Composable
fun PantallaCampana(campanaId: String, onVolver: () -> Unit) {
    val contexto = LocalContext.current
    var datos by remember { mutableStateOf<DatosCampana?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    // Llamada autenticada: el token guardado acompaña a la petición.
    remember {
        val token = AlmacenSesiones(contexto).token()
        if (token == null) {
            mensaje = "Sesión caducada o no válida. Vuelve a iniciar sesión."
        } else {
            ServicioCuentas.cargarCampana(token, campanaId) { cargada, texto ->
                datos = cargada
                mensaje = texto
            }
        }
        true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        datos?.let {
            Text(
                text = "Campaña ${it.nombre}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextoMarron
            )
            if (it.estado == "cerrada") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Campaña cerrada: solo consulta.\nNo se pueden añadir labores.",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoMarron,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Datos oficiales de la cooperativa",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                FilaDato(etiqueta = "Kilos entregados", valor = it.kilosEntregados)
                Spacer(modifier = Modifier.height(12.dp))
                FilaDato(etiqueta = "Rendimiento graso", valor = it.rendimientoGraso)
                Spacer(modifier = Modifier.height(12.dp))
                FilaDato(etiqueta = "Aceite obtenido", valor = it.aceiteObtenido)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Mi cuaderno",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, VerdeOliva, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (it.labores.isEmpty()) {
                    Text(
                        text = "Todavía no has apuntado labores en esta campaña.",
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = TextoMarron,
                        textAlign = TextAlign.Center
                    )
                } else {
                    for ((indice, labor) in it.labores.withIndex()) {
                        if (indice > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        FilaLabor(labor)
                    }
                }
            }
        } ?: Text(
            text = "Cargando los datos de la campaña…",
            fontSize = 18.sp,
            color = TextoMarron
        )
        mensaje?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoError,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        BotonSecundario(texto = "Volver al panel", alPulsar = onVolver)
    }
}

/** Una fila de "Mi cuaderno": fecha y categoría, con sus campos legibles de un vistazo. */
@Composable
fun FilaLabor(
    labor: DatosLabor,
    alEditar: (() -> Unit)? = null,
    alEliminar: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = labor.fecha + " · " + labor.categoria,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        if (labor.parcela.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            FilaCampoLabor(etiqueta = "Parcela", valor = labor.parcela)
        }
        for ((etiqueta, valor) in labor.campos) {
            Spacer(modifier = Modifier.height(4.dp))
            FilaCampoLabor(etiqueta = etiqueta, valor = valor)
        }
        if (labor.observaciones.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = labor.observaciones,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron
            )
        }
        if (alEditar != null) {
            Spacer(modifier = Modifier.height(12.dp))
            BotonSecundario(texto = "Editar", alPulsar = alEditar)
        }
        if (alEliminar != null) {
            Spacer(modifier = Modifier.height(8.dp))
            BotonPeligro(texto = "Eliminar", alPulsar = alEliminar)
        }
    }
}

/** Un campo tipo SIEX de la labor: etiqueta en negrita y su valor. */
@Composable
fun FilaCampoLabor(etiqueta: String, valor: String) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$etiqueta: ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Text(
            text = valor,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron
        )
    }
}

/** Una fila del bloque de datos oficiales: etiqueta grande y valor destacado. */
@Composable
fun FilaDato(etiqueta: String, valor: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            fontSize = 18.sp,
            color = TextoMarron
        )
        Text(
            text = valor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
    }
}

/** Datos oficiales del panel, ya formateados para enseñar (formato español). */
class DatosPanel(
    val campana: String,
    val kilosEntregados: String,
    val rendimientoGraso: String,
    val aceiteObtenido: String
)

/** Resumen de una campaña en el listado del panel. */
class DatosCampanaResumen(val id: String, val nombre: String, val estado: String)

/** Detalle de una campaña en modo consulta (labores aparte de lo oficial). */
class DatosCampana(
    val nombre: String,
    val estado: String,
    val kilosEntregados: String,
    val rendimientoGraso: String,
    val aceiteObtenido: String,
    val labores: List<DatosLabor>
)

/** Una labor de "Mi cuaderno", ya formateada para enseñar. */
class DatosLabor(
    val fecha: String,
    val categoria: String,
    val parcela: String,
    val campos: List<Pair<String, String>>,
    val observaciones: String,
    val editable: LaborEditable
)

/** Los datos de una labor tal cual para poder editarla ('Corrección manual'). */
class LaborEditable(
    val id: String,
    val fechaVisible: String,
    val fechaIso: String,
    val parcelaId: String,
    val parcelaNombre: String,
    val categoria: String,
    val campos: org.json.JSONObject,
    val observaciones: String
)

/** Un grupo de "Mi cuaderno": una campaña con sus labores. */
class DatosCampanaDeLabores(
    val nombre: String,
    val esActual: Boolean,
    val labores: List<DatosLabor>
)

/** Una parcela del socio. */
class DatosParcela(val id: String, val nombre: String)

/** Números en formato español: 10.958 y 17,93. */
private fun formatearNumero(valor: Double, decimales: Int): String {
    val formato = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-ES"))
    formato.minimumFractionDigits = decimales
    formato.maximumFractionDigits = decimales
    return formato.format(valor)
}

/** Cantidades con los decimales justos: 3, 2,5 y 850. */
private fun formatearCantidad(valor: Double): String {
    val formato = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-ES"))
    formato.minimumFractionDigits = 0
    formato.maximumFractionDigits = 2
    return formato.format(valor)
}

/** Fecha ISO (AAAA-MM-DD) a formato español (DD/MM/AAAA). */
private fun formatearFecha(iso: String): String {
    val partes = iso.split("-")
    if (partes.size != 3) return iso
    return partes[2] + "/" + partes[1] + "/" + partes[0]
}

private val NOMBRES_DE_CATEGORIA = mapOf(
    "tratamiento" to "Tratamiento fitosanitario",
    "riego" to "Riego",
    "cosecha" to "Cosecha"
)

/** Una labor del backend a texto para "Mi cuaderno". */
private fun formatearLabor(labor: org.json.JSONObject): DatosLabor {
    val campos = labor.optJSONObject("campos") ?: org.json.JSONObject()
    val parcela = labor.optJSONObject("parcela")?.optString("nombre") ?: ""
    val parejas = when (labor.optString("categoria")) {
        "tratamiento" -> listOf(
            "Producto" to campos.optString("producto"),
            "Plaga a controlar" to campos.optString("plaga_a_controlar"),
            "Cantidad" to formatearCantidad(campos.optDouble("cantidad")) +
                " " + campos.optString("unidad")
        )
        "riego" -> listOf(
            "Cantidad" to formatearCantidad(campos.optDouble("cantidad")) +
                " " + campos.optString("unidad")
        )
        "cosecha" -> listOf(
            "Modalidad" to campos.optString("modalidad"),
            "Kilos" to formatearNumero(campos.optDouble("kilos"), 0) + " kg"
        )
        else -> emptyList()
    }
    return DatosLabor(
        fecha = formatearFecha(labor.optString("fecha")),
        categoria = NOMBRES_DE_CATEGORIA[labor.optString("categoria")]
            ?: labor.optString("categoria"),
        parcela = parcela,
        campos = parejas,
        observaciones = labor.optString("observaciones"),
        editable = LaborEditable(
            id = labor.optString("id"),
            fechaVisible = formatearFecha(labor.optString("fecha")),
            fechaIso = labor.optString("fecha"),
            parcelaId = labor.optJSONObject("parcela")?.optString("id") ?: "",
            parcelaNombre = parcela,
            categoria = labor.optString("categoria"),
            campos = campos,
            observaciones = labor.optString("observaciones")
        )
    )
}

/** Un número guardado en los campos de la labor, para prellenar el formulario. */
private fun numeroInicial(labor: LaborEditable?, clave: String): String {
    val campos = labor?.campos ?: return ""
    if (!campos.has(clave)) return ""
    return formatearCantidad(campos.optDouble(clave))
}

@Composable
fun BotonPrincipal(texto: String, habilitado: Boolean = true, alPulsar: () -> Unit) {
    Button(
        onClick = alPulsar,
        enabled = habilitado,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VerdeOliva,
            contentColor = Color.White
        )
    ) {
        Text(text = texto, fontSize = 20.sp)
    }
}

@Composable
fun BotonSecundario(texto: String, alPulsar: () -> Unit) {
    OutlinedButton(
        onClick = alPulsar,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = texto, fontSize = 20.sp, color = VerdeOliva)
    }
}

/** Botón grande de acción destructiva (borrado directo, sin papelera). */
@Composable
fun BotonPeligro(texto: String, alPulsar: () -> Unit) {
    OutlinedButton(
        onClick = alPulsar,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, TextoError)
    ) {
        Text(text = texto, fontSize = 20.sp, color = TextoError)
    }
}

@Composable
fun CampoDeTexto(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    tipo: KeyboardType,
    oculto: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta, fontSize = 18.sp) },
        singleLine = true,
        visualTransformation = if (oculto) PasswordVisualTransformation() else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = tipo),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 18.sp,
            color = TextoMarron
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
    )
}

/**
 * Almacenamiento seguro del token de sesión: preferencias cifradas con una
 * clave del Keystore del dispositivo (AES-256).
 */
class AlmacenSesiones(contexto: Context) {
    private val preferencias = EncryptedSharedPreferences.create(
        contexto,
        "olivapp-sesiones",
        MasterKey.Builder(contexto)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun guardar(token: String) {
        preferencias.edit().putString("token", token).apply()
    }

    fun token(): String? = preferencias.getString("token", null)

    fun hayToken(): Boolean = token() != null

    fun borrar() {
        preferencias.edit().remove("token").apply()
    }
}

/** Llamadas al backend para cuentas y sesión. Responde siempre en el hilo principal. */
object ServicioCuentas {
    private val cliente = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val principal = Handler(Looper.getMainLooper())

    fun crearCuenta(
        email: String,
        contrasena: String,
        codigoSocio: String,
        alTerminar: (Boolean, String) -> Unit
    ) {
        val cuerpo = JSONObject()
            .put("email", email)
            .put("contrasena", contrasena)
            .put("codigo_socio", codigoSocio)
            .toString()
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/registro")
            .post(cuerpo.toRequestBody(JSON))
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(false, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val exitosa = it.isSuccessful
                    val detalle = if (exitosa) {
                        "Cuenta creada. Ya puedes iniciar sesión."
                    } else {
                        detalleDeError(it).ifEmpty { "No se pudo crear la cuenta. Revisa los datos." }
                    }
                    principal.post { alTerminar(exitosa, detalle) }
                }
            }
        })
    }

    /** Inicia sesión; devuelve el token (o null) y un mensaje claro. */
    fun iniciarSesion(email: String, contrasena: String, alTerminar: (String?, String) -> Unit) {
        val cuerpo = JSONObject()
            .put("email", email)
            .put("contrasena", contrasena)
            .toString()
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/login")
            .post(cuerpo.toRequestBody(JSON))
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val token = JSONObject(it.body?.string() ?: "").optString("token")
                        principal.post { alTerminar(token, "") }
                    } else {
                        val detalle = detalleDeError(it)
                            .ifEmpty { "No se pudo iniciar sesión. Revisa los datos." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Consulta los datos del panel del socio (llamada autenticada). */
    fun cargarMiPanel(token: String, alTerminar: (DatosPanel?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mi-panel")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val cuerpo = JSONObject(it.body?.string() ?: "")
                        val campana = cuerpo.optJSONObject("campana")?.optString("nombre") ?: ""
                        val oficiales = cuerpo.optJSONObject("datos_oficiales") ?: JSONObject()
                        val datos = DatosPanel(
                            campana = campana,
                            kilosEntregados = formatearNumero(
                                oficiales.optDouble("kilos_entregados"), 0
                            ) + " kg",
                            rendimientoGraso = formatearNumero(
                                oficiales.optDouble("rendimiento_graso"), 2
                            ) + " %",
                            aceiteObtenido = formatearNumero(
                                oficiales.optDouble("aceite_obtenido"), 0
                            ) + " kg"
                        )
                        principal.post { alTerminar(datos, "") }
                    } else {
                        val detalle = detalleDeError(it)
                            .ifEmpty { "No se pudieron cargar tus datos." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Guarda una labor del socio (llamada autenticada). */
    fun guardarLabor(token: String, cuerpo: JSONObject, alTerminar: (String?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mis-labores")
            .header("Authorization", "Bearer $token")
            .post(cuerpo.toString().toRequestBody(JSON))
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val respuesta = JSONObject(it.body?.string() ?: "")
                        val campana = respuesta.optString("campana_id")
                        principal.post {
                            alTerminar(
                                campana,
                                "Labor guardada en la campaña " +
                                    campana.replace("-", "/") + "."
                            )
                        }
                    } else {
                        val detalle = detalleDeError(it)
                            .ifEmpty { "No se pudo guardar la labor." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /**
     * Envía una nota de voz al backend para transcribirla (llamada autenticada).
     *
     * El tercer parámetro del aviso indica si el fallo fue de conexión (la nota
     * debe quedarse en la cola local, feature cola-offline-de-notas) o del
     * servidor.
     */
    fun transcribirNota(
        token: String,
        fichero: java.io.File,
        alTerminar: (String?, String, Boolean) -> Unit
    ) {
        val cuerpo = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                fichero.name,
                fichero.asRequestBody("audio/mp4".toMediaType())
            )
            .build()
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mis-notas-voz/transcripcion")
            .header("Authorization", "Bearer $token")
            .post(cuerpo)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.", true)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val respuesta = JSONObject(it.body?.string() ?: "")
                        val texto = respuesta.optString("texto")
                        principal.post { alTerminar(texto, "", false) }
                    } else {
                        val detalle = detalleDeError(it)
                            .ifEmpty { "No se pudo transcribir la nota." }
                        principal.post { alTerminar(null, detalle, false) }
                    }
                }
            }
        })
    }

    /** Consulta las parcelas del socio (llamada autenticada). */
    fun cargarParcelas(token: String, alTerminar: (List<DatosParcela>?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mis-parcelas")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val arreglo = JSONArray(it.body?.string() ?: "[]")
                        val lista = mutableListOf<DatosParcela>()
                        for (i in 0 until arreglo.length()) {
                            val p = arreglo.optJSONObject(i) ?: continue
                            lista.add(DatosParcela(p.optString("id"), p.optString("nombre")))
                        }
                        principal.post { alTerminar(lista, "") }
                    } else {
                        val detalle = detalleDeError(it)
                            .ifEmpty { "No se pudieron cargar tus parcelas." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Edita cualquier campo de una labor existente (llamada autenticada). */
    fun actualizarLabor(
        token: String,
        id: String,
        cuerpo: JSONObject,
        alTerminar: (Boolean, String) -> Unit
    ) {
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mis-labores/" + id)
            .header("Authorization", "Bearer $token")
            .put(cuerpo.toString().toRequestBody(JSON))
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(false, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val exitosa = it.isSuccessful
                    val detalle = if (exitosa) {
                        "Cambios guardados en tu cuaderno."
                    } else {
                        detalleDeError(it).ifEmpty { "No se pudieron guardar los cambios." }
                    }
                    principal.post { alTerminar(exitosa, detalle) }
                }
            }
        })
    }

    /** Elimina una labor existente: borrado directo, sin papelera (llamada autenticada). */
    fun eliminarLabor(token: String, id: String, alTerminar: (Boolean, String) -> Unit) {
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mis-labores/" + id)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(false, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val exitosa = it.isSuccessful
                    val detalle = if (exitosa) {
                        "Labor eliminada de tu cuaderno."
                    } else {
                        detalleDeError(it).ifEmpty { "No se pudo eliminar la labor." }
                    }
                    principal.post { alTerminar(exitosa, detalle) }
                }
            }
        })
    }

    /** Carga "Mi cuaderno": las labores del socio agrupadas por campaña. */
    fun cargarMisLabores(token: String, alTerminar: (List<DatosCampanaDeLabores>?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mis-labores")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val cuerpo = JSONObject(it.body?.string() ?: "{}")
                        val arreglo = cuerpo.optJSONArray("campanas") ?: JSONArray()
                        val lista = mutableListOf<DatosCampanaDeLabores>()
                        for (i in 0 until arreglo.length()) {
                            val c = arreglo.optJSONObject(i) ?: continue
                            val filas = c.optJSONArray("labores") ?: JSONArray()
                            val labores = mutableListOf<DatosLabor>()
                            for (j in 0 until filas.length()) {
                                val l = filas.optJSONObject(j) ?: continue
                                labores.add(formatearLabor(l))
                            }
                            lista.add(
                                DatosCampanaDeLabores(
                                    nombre = c.optString("nombre"),
                                    esActual = c.optBoolean("es_actual"),
                                    labores = labores
                                )
                            )
                        }
                        principal.post { alTerminar(lista, "") }
                    } else {
                        val detalle = detalleDeError(it)
                            .ifEmpty { "No se pudo cargar tu cuaderno." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Crea una parcela del socio (llamada autenticada). */
    fun crearParcela(token: String, nombre: String, alTerminar: (Boolean, String) -> Unit) {
        val cuerpo = JSONObject().put("nombre", nombre).toString()
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mis-parcelas")
            .header("Authorization", "Bearer $token")
            .post(cuerpo.toRequestBody(JSON))
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(false, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val exitosa = it.isSuccessful
                    val detalle = if (exitosa) {
                        "Parcela creada."
                    } else {
                        detalleDeError(it).ifEmpty { "No se pudo crear la parcela." }
                    }
                    principal.post { alTerminar(exitosa, detalle) }
                }
            }
        })
    }

    /** Consulta el listado de campañas del socio (llamada autenticada). */
    fun cargarCampanas(
        token: String,
        alTerminar: (List<DatosCampanaResumen>?, String) -> Unit
    ) {
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mi-campanas")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val arreglo = JSONArray(it.body?.string() ?: "[]")
                        val lista = mutableListOf<DatosCampanaResumen>()
                        for (i in 0 until arreglo.length()) {
                            val c = arreglo.optJSONObject(i) ?: continue
                            lista.add(
                                DatosCampanaResumen(
                                    id = c.optString("id"),
                                    nombre = c.optString("nombre"),
                                    estado = c.optString("estado")
                                )
                            )
                        }
                        principal.post { alTerminar(lista, "") }
                    } else {
                        val detalle = detalleDeError(it)
                            .ifEmpty { "No se pudieron cargar tus campañas." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Consulta una campaña del socio (llamada autenticada). */
    fun cargarCampana(
        token: String,
        campanaId: String,
        alTerminar: (DatosCampana?, String) -> Unit
    ) {
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mi-campanas/$campanaId")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val cuerpo = JSONObject(it.body?.string() ?: "")
                        val campana = cuerpo.optJSONObject("campana") ?: JSONObject()
                        val oficiales = cuerpo.optJSONObject("datos_oficiales") ?: JSONObject()
                        val laboresJson = cuerpo.optJSONArray("labores") ?: JSONArray()
                        val labores = mutableListOf<DatosLabor>()
                        for (i in 0 until laboresJson.length()) {
                            val l = laboresJson.optJSONObject(i) ?: continue
                            labores.add(formatearLabor(l))
                        }
                        val datos = DatosCampana(
                            nombre = campana.optString("nombre"),
                            estado = campana.optString("estado"),
                            kilosEntregados = formatearNumero(
                                oficiales.optDouble("kilos_entregados"), 0
                            ) + " kg",
                            rendimientoGraso = formatearNumero(
                                oficiales.optDouble("rendimiento_graso"), 2
                            ) + " %",
                            aceiteObtenido = formatearNumero(
                                oficiales.optDouble("aceite_obtenido"), 0
                            ) + " kg",
                            labores = labores
                        )
                        principal.post { alTerminar(datos, "") }
                    } else {
                        val detalle = detalleDeError(it)
                            .ifEmpty { "No se pudo cargar la campaña." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Consulta la cuenta del token en un endpoint protegido (llamada autenticada). */
    fun cargarMiCuenta(token: String, alTerminar: (String?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("$URL_BASE/api/v1/mi-cuenta")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val email = JSONObject(it.body?.string() ?: "").optString("email")
                        principal.post { alTerminar(email, "") }
                    } else {
                        val detalle = detalleDeError(it)
                            .ifEmpty { "Sesión caducada o no válida. Vuelve a iniciar sesión." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    private fun detalleDeError(respuesta: Response): String {
        return try {
            JSONObject(respuesta.body?.string() ?: "").optString("detail")
        } catch (e: Exception) {
            ""
        }
    }
}
