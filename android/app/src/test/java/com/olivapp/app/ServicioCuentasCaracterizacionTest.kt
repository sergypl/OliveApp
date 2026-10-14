package com.olivapp.app

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.olivapp.app.campana.domain.DatosPanel
import com.olivapp.app.core.red.ServicioCuentas
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Tests de caracterización de `ServicioCuentas` (las llamadas de la app al
 * backend) contra un servidor HTTP real en 127.0.0.1:8000, que es justo donde
 * apunta el código (vía `adb reverse`).
 *
 * Fijan antes del refactor: cada llamada autenticada lleva el token de la
 * sesión (aislamiento por socio del lado de la app), el formato español de los
 * datos oficiales y la distinción de fallos de la transcripción (400 audio
 * inválido, 502 fallo reintentable, sin conexión).
 *
 * Si el puerto 8000 está ocupado (por ejemplo, el backend de desarrollo
 * arrancado), los tests se saltan con un aviso en lugar de fallar.
 */
@RunWith(RobolectricTestRunner::class)
class ServicioCuentasCaracterizacionTest {

    private lateinit var contexto: Context
    private lateinit var servidor: ServidorDePrueba
    private val autorizacion = AtomicReference<String?>(null)

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        servidor = try {
            ServidorDePrueba(puerto = 8000)
        } catch (e: BindException) {
            Assume.assumeNoException(
                "El puerto 8000 está ocupado (¿backend de desarrollo arrancado?): " +
                    "se salta la caracterización de red.",
                e
            )
            return
        }
        servidor.alRecibir = { peticion ->
            autorizacion.set(peticion.headers["Authorization"])
        }
        servidor.arrancar()
    }

    @After
    fun limpiar() {
        if (this::servidor.isInitialized) servidor.parar()
    }

    /** Espera a que la respuesta (publicada en el hilo principal) llegue. */
    private fun <T> esperar(obtener: () -> T?): T {
        val limite = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < limite) {
            shadowOf(Looper.getMainLooper()).idle()
            val valor = obtener()
            if (valor != null) return valor
            Thread.sleep(10)
        }
        throw AssertionError("Se agotó el tiempo esperando la respuesta del servicio.")
    }

    private fun audioDePrueba(): File {
        val fichero = File(contexto.cacheDir, "audio-prueba.m4a")
        fichero.writeBytes(byteArrayOf(0, 1, 2, 3))
        return fichero
    }

    private fun panelDePrueba(): RespuestaDePrueba = RespuestaDePrueba(
        200,
        """
        {"campana": {"nombre": "Campaña 2025/2026"},
         "datos_oficiales": {"kilos_entregados": 10958,
                             "rendimiento_graso": 17.93,
                             "aceite_obtenido": 1774}}
        """.trimIndent()
    )

    @Test
    fun `cada llamada autenticada lleva el token de la sesion`() {
        servidor.responderA("/api/v1/mi-panel", panelDePrueba())
        val resultado = AtomicReference<DatosPanel?>()

        ServicioCuentas.cargarMiPanel("token-de-prueba") { datos, _ -> resultado.set(datos) }

        esperar { resultado.get() }
        assertEquals("Bearer token-de-prueba", autorizacion.get())
    }

    @Test
    fun `los datos oficiales se formatean en espanol`() {
        servidor.responderA("/api/v1/mi-panel", panelDePrueba())
        val resultado = AtomicReference<DatosPanel?>()

        ServicioCuentas.cargarMiPanel("t") { datos, _ -> resultado.set(datos) }

        val datos = esperar { resultado.get() }
        assertEquals("Campaña 2025/2026", datos.campana)
        assertEquals("10.958 kg", datos.kilosEntregados)
        assertEquals("17,93 %", datos.rendimientoGraso)
        assertEquals("1.774 kg", datos.aceiteObtenido)
    }

    @Test
    fun `la transcripcion correcta devuelve texto y borrador`() {
        servidor.responderA(
            "/api/v1/mis-notas-voz/transcripcion",
            RespuestaDePrueba(200, """{"texto": "He regado.", "borrador": {"fecha": "2026-01-20"}}""")
        )
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.transcribirNota("t", audioDePrueba()) { texto, borrador, error, sinConexion, audioInvalido ->
            resultado.set(
                arrayOf(texto ?: "", borrador, error, sinConexion.toString(), audioInvalido.toString())
            )
        }

        val (texto, borrador, error, sinConexion, audioInvalido) = esperar { resultado.get() }
        assertEquals("He regado.", texto)
        assertEquals(
            JSONObject("""{"fecha": "2026-01-20"}""").toString(),
            JSONObject(borrador).toString()
        )
        assertEquals("", error)
        assertEquals("false", sinConexion)
        assertEquals("false", audioInvalido)
    }

    @Test
    fun `el borrador nulo de la IA se guarda con el marcador de rechazo`() {
        servidor.responderA(
            "/api/v1/mis-notas-voz/transcripcion",
            RespuestaDePrueba(200, """{"texto": "He paseado.", "borrador": null}""")
        )
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.transcribirNota("t", audioDePrueba()) { texto, borrador, _, _, _ ->
            resultado.set(arrayOf(texto ?: "", borrador))
        }

        val (texto, borrador) = esperar { resultado.get() }
        assertEquals("He paseado.", texto)
        assertEquals(AlmacenNotasVoz.BORRADOR_RECHAZADO, borrador)
    }

    @Test
    fun `el 400 del backend es audio invalido y pide repetir el dictado`() {
        servidor.responderA(
            "/api/v1/mis-notas-voz/transcripcion",
            RespuestaDePrueba(
                400,
                """{"detail": "Este audio no se puede transcribir. Vuelve a grabar la nota."}"""
            )
        )
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.transcribirNota("t", audioDePrueba()) { texto, _, error, sinConexion, audioInvalido ->
            resultado.set(
                arrayOf(texto ?: "", error, sinConexion.toString(), audioInvalido.toString())
            )
        }

        val (texto, error, sinConexion, audioInvalido) = esperar { resultado.get() }
        assertEquals("", texto)
        assertEquals("Este audio no se puede transcribir. Vuelve a grabar la nota.", error)
        assertEquals("false", sinConexion)
        assertEquals("true", audioInvalido)
    }

    @Test
    fun `el 502 del backend es fallo del servicio reintentable`() {
        servidor.responderA(
            "/api/v1/mis-notas-voz/transcripcion",
            RespuestaDePrueba(502, """{"detail": "No se ha podido transcribir. Inténtalo de nuevo."}""")
        )
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.transcribirNota("t", audioDePrueba()) { texto, _, error, sinConexion, audioInvalido ->
            resultado.set(
                arrayOf(texto ?: "", error, sinConexion.toString(), audioInvalido.toString())
            )
        }

        val (texto, error, sinConexion, audioInvalido) = esperar { resultado.get() }
        assertEquals("", texto)
        assertEquals("No se ha podido transcribir. Inténtalo de nuevo.", error)
        assertEquals("false", sinConexion)
        assertEquals("false", audioInvalido)
    }

    @Test
    fun `crear cuenta devuelve el aviso fijo de exito`() {
        servidor.responderA("/api/v1/registro", RespuestaDePrueba(201, """{"id": "socio-1"}"""))
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.crearCuenta("ana@example.com", "12345678", "Socio 1") { exitosa, texto ->
            resultado.set(arrayOf(exitosa.toString(), texto))
        }

        val (exitosa, texto) = esperar { resultado.get() }
        assertEquals("true", exitosa)
        assertEquals("Cuenta creada. Ya puedes iniciar sesión.", texto)
    }

    @Test
    fun `crear cuenta fallida devuelve el detalle del backend`() {
        servidor.responderA(
            "/api/v1/registro",
            RespuestaDePrueba(409, """{"detail": "Ese email ya está registrado. Inicia sesión."}""")
        )
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.crearCuenta("ana@example.com", "12345678", "Socio 1") { exitosa, texto ->
            resultado.set(arrayOf(exitosa.toString(), texto))
        }

        val (exitosa, texto) = esperar { resultado.get() }
        assertEquals("false", exitosa)
        assertEquals("Ese email ya está registrado. Inicia sesión.", texto)
    }

    @Test
    fun `iniciar sesion devuelve el token del backend`() {
        servidor.responderA("/api/v1/login", RespuestaDePrueba(200, """{"token": "token-abc"}"""))
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.iniciarSesion("ana@example.com", "12345678") { token, texto ->
            resultado.set(arrayOf(token ?: "", texto))
        }

        val (token, texto) = esperar { resultado.get() }
        assertEquals("token-abc", token)
        assertEquals("", texto)
    }

    @Test
    fun `iniciar sesion fallido devuelve el detalle del backend`() {
        servidor.responderA(
            "/api/v1/login",
            RespuestaDePrueba(401, """{"detail": "Email o contraseña incorrectos."}""")
        )
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.iniciarSesion("ana@example.com", "mal") { token, texto ->
            resultado.set(arrayOf(token ?: "", texto))
        }

        val (token, texto) = esperar { resultado.get() }
        assertEquals("", token)
        assertEquals("Email o contraseña incorrectos.", texto)
    }

    @Test
    fun `sin servidor iniciar sesion avisa de que no hay conexion`() {
        servidor.parar()
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.iniciarSesion("ana@example.com", "12345678") { token, texto ->
            resultado.set(arrayOf(token ?: "", texto))
        }

        val (token, texto) = esperar { resultado.get() }
        assertEquals("", token)
        assertEquals("No hay conexión con el servidor. Inténtalo de nuevo.", texto)
    }

    @Test
    fun `sin servidor la nota se marca como sin conexion`() {
        servidor.parar()
        val resultado = AtomicReference<Array<String>?>()

        ServicioCuentas.transcribirNota("t", audioDePrueba()) { texto, _, error, sinConexion, audioInvalido ->
            resultado.set(
                arrayOf(texto ?: "", error, sinConexion.toString(), audioInvalido.toString())
            )
        }

        val (texto, error, sinConexion, audioInvalido) = esperar { resultado.get() }
        assertEquals("", texto)
        assertEquals("No hay conexión con el servidor. Inténtalo de nuevo.", error)
        assertEquals("true", sinConexion)
        assertEquals("false", audioInvalido)
    }
}

/** Respuesta fija del servidor de prueba. */
class RespuestaDePrueba(val codigo: Int, val cuerpo: String)

/** Petición recibida por el servidor de prueba (solo lo que hace falta). */
class PeticionDePrueba(val headers: Map<String, String>)

/**
 * Mini servidor HTTP sobre ServerSocket para el puerto 8000: devuelve respuestas
 * fijas por ruta sin añadir dependencias de test.
 */
class ServidorDePrueba(private val puerto: Int) {

    var alRecibir: ((PeticionDePrueba) -> Unit)? = null

    private val respuestas = mutableMapOf<String, RespuestaDePrueba>()
    private var socket: ServerSocket? = null
    private var hilo: Thread? = null

    fun responderA(ruta: String, respuesta: RespuestaDePrueba) {
        respuestas[ruta] = respuesta
    }

    fun arrancar() {
        val servidor = ServerSocket(puerto)
        socket = servidor
        hilo = Thread {
            while (!servidor.isClosed) {
                val conexion = try {
                    servidor.accept()
                } catch (e: Exception) {
                    return@Thread
                }
                atender(conexion)
            }
        }.also { it.isDaemon = true; it.start() }
    }

    fun parar() {
        try {
            socket?.close()
        } catch (e: Exception) {
            // El socket ya estaba cerrado.
        }
        socket = null
    }

    private fun atender(conexion: Socket) {
        conexion.use { c ->
            val entrada = BufferedReader(InputStreamReader(c.getInputStream(), StandardCharsets.ISO_8859_1))
            val lineaDePeticion = entrada.readLine() ?: return
            val ruta = lineaDePeticion.split(" ").getOrElse(1) { "/" }.substringBefore("?")
            val headers = mutableMapOf<String, String>()
            while (true) {
                val linea = entrada.readLine() ?: break
                if (linea.isEmpty()) break
                val dosPuntos = linea.indexOf(':')
                if (dosPuntos > 0) {
                    headers[linea.substring(0, dosPuntos).trim()] = linea.substring(dosPuntos + 1).trim()
                }
            }
            val longitud = headers["Content-Length"]?.toIntOrNull() ?: 0
            if (longitud > 0) {
                val cuerpo = CharArray(longitud)
                var leidos = 0
                while (leidos < longitud) {
                    val n = entrada.read(cuerpo, leidos, longitud - leidos)
                    if (n < 0) break
                    leidos += n
                }
            }
            alRecibir?.invoke(PeticionDePrueba(headers))
            val respuesta = respuestas[ruta]
                ?: RespuestaDePrueba(404, """{"detail": "ruta de prueba no registrada"}""")
            val bytes = respuesta.cuerpo.toByteArray(StandardCharsets.UTF_8)
            val cabecera = "HTTP/1.1 ${respuesta.codigo} ${if (respuesta.codigo == 200) "OK" else "Error"}\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
            c.getOutputStream().write(cabecera.toByteArray(StandardCharsets.ISO_8859_1))
            c.getOutputStream().write(bytes)
            c.getOutputStream().flush()
        }
    }
}
