package com.olivapp.app.simulador.data

import android.os.Handler
import android.os.Looper
import com.olivapp.app.core.red.ClienteRed
import com.olivapp.app.simulador.domain.MercadoAceite
import com.olivapp.app.simulador.domain.PreciosAceiteRepositorio
import com.olivapp.app.simulador.domain.PreciosDeReferencia
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/**
 * Precios de referencia del aceite contra el backend (`/api/v1/precios-aceite`).
 * La app nunca llama a la API de la Comisión Europea: lo hace el backend, que
 * además guarda una caché. Responde siempre en el hilo principal.
 */
class PreciosAceiteRepositorioImpl : PreciosAceiteRepositorio {

    private val principal = Handler(Looper.getMainLooper())

    override fun cargarSemanas(
        token: String,
        mercado: MercadoAceite,
        campana: String,
        alTerminar: (PreciosDeReferencia?, String) -> Unit
    ) {
        val url = "${ClienteRed.URL_BASE}/api/v1/precios-aceite".toHttpUrl().newBuilder()
            .addQueryParameter("mercado", mercado.codigo)
            .addQueryParameter("campana", campana)
            .build()
        pedir(token, url, alTerminar) { InterpretePrecios.semanas(it) }
    }

    override fun cargarUltimaSemana(
        token: String,
        mercado: MercadoAceite,
        alTerminar: (PreciosDeReferencia?, String) -> Unit
    ) {
        val url = "${ClienteRed.URL_BASE}/api/v1/precios-aceite/ultima-semana".toHttpUrl()
            .newBuilder()
            .addQueryParameter("mercado", mercado.codigo)
            .build()
        pedir(token, url, alTerminar) { InterpretePrecios.ultimaSemana(it) }
    }

    private fun pedir(
        token: String,
        url: okhttp3.HttpUrl,
        alTerminar: (PreciosDeReferencia?, String) -> Unit,
        interpretar: (JSONObject) -> PreciosDeReferencia?
    ) {
        val peticion = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post { alTerminar(null, SIN_CONEXION) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val referencia = try {
                            interpretar(JSONObject(it.body?.string() ?: ""))
                        } catch (e: Exception) {
                            null
                        }
                        principal.post {
                            if (referencia != null) {
                                alTerminar(referencia, "")
                            } else {
                                alTerminar(null, ILEGIBLE)
                            }
                        }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it).ifEmpty { ILEGIBLE }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    private companion object {
        const val SIN_CONEXION =
            "No hay conexión con el servidor. Puedes escribir el precio a mano."
        const val ILEGIBLE =
            "No se han podido cargar los precios. Puedes escribir el precio a mano."
    }
}
