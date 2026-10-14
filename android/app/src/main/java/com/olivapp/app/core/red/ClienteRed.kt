package com.olivapp.app.core.red

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject

/**
 * Cliente HTTP compartido y configuración del backend. Los endpoints no cambian
 * en el refactor de arquitectura.
 */
object ClienteRed {

    // Backend en desarrollo: cada dispositivo (emulador o móvil físico) alcanza el
    // Mac del host por su propio localhost gracias a `adb reverse tcp:8000 tcp:8000`
    // (init.sh lo configura en cada dispositivo conectado).
    const val URL_BASE = "http://127.0.0.1:8000"

    val cliente = OkHttpClient()

    val json = "application/json; charset=utf-8".toMediaType()

    /** Detalle de error que devuelve el backend (campo `detail` del JSON). */
    fun detalleDeError(respuesta: Response): String {
        return try {
            JSONObject(respuesta.body?.string() ?: "").optString("detail")
        } catch (e: Exception) {
            ""
        }
    }
}
