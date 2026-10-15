package com.olivapp.app.voz.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.olivapp.app.acceso.data.AlmacenSesiones
import com.olivapp.app.core.red.ServicioCuentas
import com.olivapp.app.voz.domain.aplicarTranscripcion
import java.io.File

/**
 * Sincroniza la cola de notas pendientes (feature
 * sincronizacion-pendientes-al-reconectar).
 *
 * Al recuperar la conexión, las notas pendientes se envían a transcribir
 * automáticamente en orden de grabación, una detrás de otra y sin duplicados.
 * Los estados cambian en Room y `alCambiar` avisa a la pantalla de notas si
 * está abierta.
 */
object SincronizadorNotas {
    /** Aviso a la pantalla de notas cuando la cola cambia (si está abierta). */
    var alCambiar: (() -> Unit)? = null

    private val principal = Handler(Looper.getMainLooper())
    private var sincronizando = false

    /** Envía las notas pendientes en orden de grabación (si hay sesión). */
    fun sincronizar(contexto: Context) {
        principal.post {
            if (sincronizando) return@post
            val sesion = AlmacenSesiones(contexto)
            val token = sesion.token() ?: return@post
            // Solo se envía la cola de la cuenta activa y con su token: sin
            // cuenta identificada no sale nada (nada de otros socios).
            val idDeLaCuenta = sesion.idDeCuenta() ?: return@post
            val almacen = AlmacenNotasVoz(contexto) { idDeLaCuenta }
            val ids = almacen.pendientesEnOrdenDeGrabacion().map { it.id }
            if (ids.isEmpty()) return@post
            sincronizando = true
            enviarSiguiente(almacen, token, ids, 0)
        }
    }

    private fun enviarSiguiente(
        almacen: AlmacenNotasVoz,
        token: String,
        ids: List<String>,
        indice: Int
    ) {
        if (indice >= ids.size) {
            sincronizando = false
            return
        }
        // Estado comprobado justo antes de enviar: si otra ruta ya tomó la nota
        // (o ya no está pendiente), se salta para no duplicar transcripciones.
        val nota = almacen.notas().firstOrNull { it.id == ids[indice] }
        if (nota == null || nota.estado != "pendiente") {
            enviarSiguiente(almacen, token, ids, indice + 1)
            return
        }
        almacen.marcar(nota.id, "transcribiendo", "")
        alCambiar?.invoke()
        ServicioCuentas.transcribirNota(
            token,
            File(nota.fichero)
        ) { texto, borrador, error, sinConexion, audioInvalido ->
            aplicarTranscripcion(
                almacen,
                nota.id,
                texto,
                borrador,
                error,
                sinConexion,
                audioInvalido
            )
            alCambiar?.invoke()
            if (sinConexion) {
                // Sin conexión se detiene la cola: las notas siguen pendientes
                // y se reanudará al recuperar la conexión.
                sincronizando = false
            } else {
                enviarSiguiente(almacen, token, ids, indice + 1)
            }
        }
    }
}
