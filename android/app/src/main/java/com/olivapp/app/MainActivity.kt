package com.olivapp.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
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
    var pantalla by remember { mutableStateOf("inicio") }
    when (pantalla) {
        "registro" -> PantallaRegistro(onVolver = { pantalla = "inicio" })
        else -> PantallaBienvenida(onCrearCuenta = { pantalla = "registro" })
    }
}

@Composable
fun PantallaBienvenida(onCrearCuenta: () -> Unit) {
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
        BotonPrincipal(texto = "Crear cuenta", alPulsar = onCrearCuenta)
    }
}

@Composable
fun PantallaRegistro(onVolver: () -> Unit) {
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
    }
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

/** Llamada al backend para crear la cuenta. Responde siempre en el hilo principal. */
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
                        val texto = try {
                            JSONObject(it.body?.string() ?: "").optString("detail")
                        } catch (e: Exception) {
                            ""
                        }
                        texto.ifEmpty { "No se pudo crear la cuenta. Revisa los datos." }
                    }
                    principal.post { alTerminar(exitosa, detalle) }
                }
            }
        })
    }
}
