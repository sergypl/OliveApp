package com.olivapp.app.cuaderno.domain

import org.json.JSONObject

/**
 * Labores del cuaderno de campo del socio (alta, edición, borrado y consulta
 * agrupada por campaña). Responde siempre en el hilo principal (vía el gateway
 * HTTP).
 */
interface LaboresRepositorio {

    /** "Mi cuaderno": labores agrupadas por campaña, de la más reciente a la más antigua. */
    fun cargarMisLabores(
        token: String,
        alTerminar: (lista: List<DatosCampanaDeLabores>?, mensaje: String) -> Unit
    )

    /** Alta de una labor: devuelve la campaña en la que ha quedado guardada. */
    fun guardarLabor(
        token: String,
        cuerpo: JSONObject,
        alTerminar: (campana: String?, mensaje: String) -> Unit
    )

    /** Edición de una labor existente ('Corrección manual'). */
    fun actualizarLabor(
        token: String,
        id: String,
        cuerpo: JSONObject,
        alTerminar: (exito: Boolean, mensaje: String) -> Unit
    )

    /** Borrado directo (no hay papelera en el MVP). */
    fun eliminarLabor(
        token: String,
        id: String,
        alTerminar: (exito: Boolean, mensaje: String) -> Unit
    )
}
