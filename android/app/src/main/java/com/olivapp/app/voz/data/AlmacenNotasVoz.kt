package com.olivapp.app.voz.data

import android.content.Context
import com.olivapp.app.voz.domain.ColaDeNotas
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray

/**
 * La cola de notas de voz en el teléfono (Room y ficheros de audio).
 *
 * La cola es del dispositivo, pero cada nota pertenece a la cuenta de su socio
 * (feature aislamiento-de-cola-por-socio): con dos cuentas en el mismo
 * teléfono, cada socio ve y envía solo las suyas. La identidad de la cuenta
 * activa llega por [duenoDeLaCola] (en producción, la de `AlmacenSesiones`).
 */
class AlmacenNotasVoz(
    private val contexto: Context,
    private val duenoDeLaCola: () -> String? = { null }
) : ColaDeNotas {

    class NotaDeVoz(
        val id: String,
        val fichero: String,
        val cuando: String,
        val segundos: Int,
        val estado: String,
        val texto: String,
        val borrador: String
    ) {
        val estadoVisible: String
            get() = when (estado) {
                "pendiente" -> "Pendiente de enviar. Está en la cola del teléfono."
                "transcribiendo" -> "Transcribiendo…"
                "transcrita" -> "Transcrita"
                "confirmada" -> "Confirmada. La labor ya está en Mi cuaderno."
                "descartada" -> "Descartada. No se ha creado ninguna labor."
                "error" -> "No se ha podido transcribir. Toca para reintentar."
                "audio_invalido" -> "Este audio no se puede transcribir. Vuelve a grabar la nota."
                else -> "Pendiente de transcripción"
            }
    }

    companion object {
        /** Marcador guardado cuando la IA no reconoce la categoría de la labor. */
        const val BORRADOR_RECHAZADO = """{"rechazado": true}"""
    }

    private val carpeta = File(contexto.filesDir, "notas-voz")
    private val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val dao = BaseNotasVoz.de(contexto).dao()

    init {
        importarIndiceAntiguo()
    }

    override fun nuevaRutaDeAudio(): File {
        carpeta.mkdirs()
        return File(carpeta, "nota-" + System.currentTimeMillis() + ".m4a")
    }

    /** Registra una nota grabada en la cola (la más reciente la primera). */
    override fun guardar(fichero: File, segundos: Int): NotaDeVoz {
        val ahora = LocalDateTime.now()
        dao.insertar(
            NotaVozFila(
                id = fichero.name,
                fichero = fichero.absolutePath,
                creadaEn = ahora.toString(),
                segundos = segundos,
                estado = "pendiente",
                texto = "",
                dueno = duenoActual()
            )
        )
        return NotaDeVoz(
            id = fichero.name,
            fichero = fichero.absolutePath,
            cuando = ahora.format(formato),
            segundos = segundos,
            estado = "pendiente",
            texto = "",
            borrador = ""
        )
    }

    /** Cambia el estado de una nota (y guarda texto y borrador cuando llegan). */
    override fun marcar(id: String, estado: String, texto: String, borrador: String) {
        dao.marcar(id, estado, texto, borrador)
    }

    /** Guarda el borrador IA de una nota sin tocar su estado ni su texto. */
    override fun actualizarBorrador(id: String, borrador: String) {
        dao.actualizarBorrador(id, borrador)
    }

    override fun notas(): List<NotaDeVoz> = dao.notas(duenoActual()).map { aNotaDeVoz(it) }

    /** Notas pendientes de enviar, de la más antigua a la más reciente. */
    fun pendientesEnOrdenDeGrabacion(): List<NotaDeVoz> =
        dao.pendientes(duenoActual()).map { aNotaDeVoz(it) }

    /**
     * Reinicio seguro tras un corte del proceso: una nota que se quedó
     * 'transcribiendo' (sin respuesta del backend) vuelve a la cola como
     * 'pendiente' para que se reintente. Es idempotente y no toca las notas en
     * ningún otro estado.
     */
    fun reanudarTranscripcionesPendientes() {
        dao.reanudarTranscripciones()
    }

    /**
     * Dueño de la cola en este momento. Sin cuenta identificada (o con una nota
     * grabada sin sesión) queda vacío: esas notas son de nadie.
     */
    private fun duenoActual(): String = duenoDeLaCola() ?: ""

    private fun aNotaDeVoz(fila: NotaVozFila): NotaDeVoz {
        val creada = try {
            LocalDateTime.parse(fila.creadaEn).format(formato)
        } catch (e: Exception) {
            fila.creadaEn
        }
        return NotaDeVoz(
            id = fila.id,
            fichero = fila.fichero,
            cuando = creada,
            segundos = fila.segundos,
            estado = fila.estado,
            texto = fila.texto,
            borrador = fila.borrador
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
