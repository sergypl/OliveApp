package com.olivapp.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

// Dirección visual de DESIGN.md: fondo crema, texto marrón oscuro, cuerpo 18sp mínimo.
private val FondoCrema = Color(0xFFFAF7F0)
private val TextoMarron = Color(0xFF2E2A20)
private val VerdeOliva = Color(0xFF5A6E3A)
private val TextoError = Color(0xFF8B1E1E)

// Backend en desarrollo: el emulador alcanza el Mac del host por 10.0.2.2.
private const val URL_BASE = "http://10.0.2.2:8000"

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
            onAbrirCampana = { id ->
                idCampana = id
                pantalla = "campana"
            }
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
            text = "Con tu email y una contraseña.",
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
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(
            texto = if (trabajando) "Creando la cuenta…" else "Crear cuenta",
            habilitado = !trabajando,
            alPulsar = {
                trabajando = true
                mensaje = null
                registrada = false
                ServicioCuentas.crearCuenta(email, contrasena) { exitosa, texto ->
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
fun PantallaPanel(onSesionCerrada: () -> Unit, onAbrirCampana: (String) -> Unit) {
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
        BotonSecundario(
            texto = "Cuaderno de campo",
            alPulsar = { aviso = "El cuaderno de campo estará disponible próximamente." }
        )
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
                Text(
                    text = "Todavía no has apuntado labores en esta campaña.",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoMarron,
                    textAlign = TextAlign.Center
                )
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

/** Detalle de una campaña en modo consulta (labores aparte, aún vacías). */
class DatosCampana(
    val nombre: String,
    val estado: String,
    val kilosEntregados: String,
    val rendimientoGraso: String,
    val aceiteObtenido: String
)

/** Números en formato español: 10.958 y 17,93. */
private fun formatearNumero(valor: Double, decimales: Int): String {
    val formato = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-ES"))
    formato.minimumFractionDigits = decimales
    formato.maximumFractionDigits = decimales
    return formato.format(valor)
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

    fun crearCuenta(email: String, contrasena: String, alTerminar: (Boolean, String) -> Unit) {
        val cuerpo = JSONObject()
            .put("email", email)
            .put("contrasena", contrasena)
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
                            ) + " kg"
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
