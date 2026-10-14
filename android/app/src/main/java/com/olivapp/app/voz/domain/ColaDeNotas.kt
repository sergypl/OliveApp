package com.olivapp.app.voz.domain

import com.olivapp.app.voz.data.AlmacenNotasVoz
import java.io.File

/**
 * La cola de notas de voz del teléfono: grabar, consultar y cambiar el estado
 * de cada nota, sin red.
 *
 * Wrinkle declarado del refactor: el modelo de nota
 * (`AlmacenNotasVoz.NotaDeVoz`) se conserva anidado en el almacén para no
 * reescribir el código original (los tests lo referencian así).
 */
interface ColaDeNotas {

    /** Ruta del fichero donde grabar la próxima nota. */
    fun nuevaRutaDeAudio(): File

    /** Registra una nota grabada en la cola (la más reciente la primera). */
    fun guardar(fichero: File, segundos: Int): AlmacenNotasVoz.NotaDeVoz

    /** La cola completa, de la más reciente a la más antigua. */
    fun notas(): List<AlmacenNotasVoz.NotaDeVoz>

    /** Cambia el estado de una nota (y guarda texto y borrador cuando llegan). */
    fun marcar(id: String, estado: String, texto: String, borrador: String = "")

    /** Guarda el borrador IA de una nota sin tocar su estado ni su texto. */
    fun actualizarBorrador(id: String, borrador: String)
}
