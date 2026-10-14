package com.olivapp.app.voz.data

import com.olivapp.app.voz.domain.NotasRepositorio
import java.io.File

/**
 * Cola de notas de voz del teléfono: delega en el almacén local (Room y
 * ficheros de audio), sin red.
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
}
