package com.olivapp.app.voz.presentation

import com.olivapp.app.voz.data.AlmacenNotasVoz
import com.olivapp.app.voz.domain.GrabadorDeNotas
import com.olivapp.app.voz.domain.NotasRepositorio
import java.io.File

/**
 * Fakes de la feature de notas de voz para testear los ViewModels sin red,
 * sin Room y sin micrófono: la cola responde al momento y la transcripción se
 * puede retener para simular espera.
 */

/** Resultado de una llamada de red (la tupla del gateway HTTP). */
data class ResultadoDeRed(
    val texto: String? = null,
    val borrador: String = "",
    val error: String = "",
    val sinConexion: Boolean = false,
    val audioInvalido: Boolean = false
)

class FakeNotasRepositorio(
    private val responderAlMomento: Boolean = true
) : NotasRepositorio {

    /** La cola, de la más reciente a la más antigua (como en el almacén). */
    val cola = mutableListOf<AlmacenNotasVoz.NotaDeVoz>()

    var resultadoTranscribir = ResultadoDeRed(
        texto = "texto transcrito",
        borrador = """{"pregunta":"¿Cuántos kilos has recogido?"}"""
    )
    var resultadoCompletar = ResultadoDeRed(
        texto = "2.500 kilos de aceituna",
        borrador = """{"campos":{"cantidad":2500,"unidad":"kg"}}"""
    )

    // Llamadas recibidas.
    var marcarLlamado: List<String> = emptyList()
    var actualizarBorradorLlamado: Pair<String, String>? = null
    var transcripcionLlamada: Pair<String, File>? = null
    var completarLlamado: Triple<String, File, String>? = null
    var transcribirLlamadas = 0

    // Respuestas retenidas cuando no se responde al momento.
    var pendienteTranscribir: ((String?, String, String, Boolean, Boolean) -> Unit)? = null
    var pendienteCompletar: ((String?, String, String, Boolean, Boolean) -> Unit)? = null

    override fun nuevaRutaDeAudio(): File = File("nota-fake.m4a")

    override fun guardar(fichero: File, segundos: Int): AlmacenNotasVoz.NotaDeVoz {
        val nota = AlmacenNotasVoz.NotaDeVoz(
            id = fichero.name,
            fichero = fichero.absolutePath,
            cuando = "14/10/2026 10:00",
            segundos = segundos,
            estado = "pendiente",
            texto = "",
            borrador = ""
        )
        cola.add(0, nota)
        return nota
    }

    override fun notas(): List<AlmacenNotasVoz.NotaDeVoz> = cola.toList()

    override fun marcar(id: String, estado: String, texto: String, borrador: String) {
        marcarLlamado = listOf(id, estado, texto, borrador)
        val indice = cola.indexOfFirst { it.id == id }
        if (indice >= 0) {
            val nota = cola[indice]
            cola[indice] = AlmacenNotasVoz.NotaDeVoz(
                nota.id, nota.fichero, nota.cuando, nota.segundos, estado, texto, borrador
            )
        }
    }

    override fun actualizarBorrador(id: String, borrador: String) {
        actualizarBorradorLlamado = id to borrador
        val indice = cola.indexOfFirst { it.id == id }
        if (indice >= 0) {
            val nota = cola[indice]
            cola[indice] = AlmacenNotasVoz.NotaDeVoz(
                nota.id, nota.fichero, nota.cuando, nota.segundos,
                nota.estado, nota.texto, borrador
            )
        }
    }

    override fun transcribirNota(
        token: String,
        fichero: File,
        alTerminar: (String?, String, String, Boolean, Boolean) -> Unit
    ) {
        transcribirLlamadas += 1
        transcripcionLlamada = token to fichero
        if (responderAlMomento) {
            val resultado = resultadoTranscribir
            alTerminar(
                resultado.texto, resultado.borrador, resultado.error,
                resultado.sinConexion, resultado.audioInvalido
            )
        } else {
            pendienteTranscribir = alTerminar
        }
    }

    override fun completarBorrador(
        token: String,
        fichero: File,
        borrador: String,
        alTerminar: (String?, String, String, Boolean, Boolean) -> Unit
    ) {
        completarLlamado = Triple(token, fichero, borrador)
        if (responderAlMomento) {
            val resultado = resultadoCompletar
            alTerminar(
                resultado.texto, resultado.borrador, resultado.error,
                resultado.sinConexion, resultado.audioInvalido
            )
        } else {
            pendienteCompletar = alTerminar
        }
    }
}

/** Fake de la grabadora: no graba audio y devuelve segundos configurables. */
class FakeGrabadorDeNotas : GrabadorDeNotas {

    var segundosGrabados = 3
    var grabando = false
    var amplitudActual = 0
    var empezarLlamadas = 0

    override fun amplitud(): Int = amplitudActual

    override fun empezar(fichero: File) {
        empezarLlamadas += 1
        grabando = true
    }

    override fun parar(): Int {
        grabando = false
        return segundosGrabados
    }
}
