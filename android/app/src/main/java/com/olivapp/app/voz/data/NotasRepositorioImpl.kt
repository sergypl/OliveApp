package com.olivapp.app.voz.data

import com.olivapp.app.core.red.ServicioCuentas
import com.olivapp.app.voz.domain.NotasRepositorio
import java.io.File

/**
 * La feature de notas de voz contra el teléfono y el backend: la cola local
 * delega en el almacén (Room y ficheros) y la transcripción en el gateway HTTP
 * (`ServicioCuentas`), que responde siempre en el hilo principal.
 */
class NotasRepositorioImpl(
    private val almacen: AlmacenNotasVoz
) : NotasRepositorio {

    override fun nuevaRutaDeAudio(): File = almacen.nuevaRutaDeAudio()

    override fun guardar(fichero: File, segundos: Int): AlmacenNotasVoz.NotaDeVoz =
        almacen.guardar(fichero, segundos)

    override fun notas(): List<AlmacenNotasVoz.NotaDeVoz> = almacen.notas()

    override fun marcar(id: String, estado: String, texto: String, borrador: String) {
        almacen.marcar(id, estado, texto, borrador)
    }

    override fun actualizarBorrador(id: String, borrador: String) {
        almacen.actualizarBorrador(id, borrador)
    }

    override fun transcribirNota(
        token: String,
        fichero: File,
        alTerminar: (
            texto: String?,
            borrador: String,
            error: String,
            sinConexion: Boolean,
            audioInvalido: Boolean
        ) -> Unit
    ) {
        ServicioCuentas.transcribirNota(token, fichero, alTerminar)
    }

    override fun completarBorrador(
        token: String,
        fichero: File,
        borrador: String,
        alTerminar: (
            texto: String?,
            borrador: String,
            error: String,
            sinConexion: Boolean,
            audioInvalido: Boolean
        ) -> Unit
    ) {
        ServicioCuentas.completarBorrador(token, fichero, borrador, alTerminar)
    }
}
