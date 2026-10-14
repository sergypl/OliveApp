package com.olivapp.app.cuaderno.data

import com.olivapp.app.core.red.ServicioCuentas
import com.olivapp.app.cuaderno.domain.DatosCampanaDeLabores
import com.olivapp.app.cuaderno.domain.LaboresRepositorio
import org.json.JSONObject

/**
 * Labores del cuaderno contra el backend: delega en el gateway HTTP
 * (`ServicioCuentas`), que responde siempre en el hilo principal.
 */
class LaboresRepositorioImpl : LaboresRepositorio {

    override fun cargarMisLabores(
        token: String,
        alTerminar: (lista: List<DatosCampanaDeLabores>?, mensaje: String) -> Unit
    ) {
        ServicioCuentas.cargarMisLabores(token, alTerminar)
    }

    override fun guardarLabor(
        token: String,
        cuerpo: JSONObject,
        alTerminar: (campana: String?, mensaje: String) -> Unit
    ) {
        ServicioCuentas.guardarLabor(token, cuerpo, alTerminar)
    }

    override fun actualizarLabor(
        token: String,
        id: String,
        cuerpo: JSONObject,
        alTerminar: (exito: Boolean, mensaje: String) -> Unit
    ) {
        ServicioCuentas.actualizarLabor(token, id, cuerpo, alTerminar)
    }

    override fun eliminarLabor(
        token: String,
        id: String,
        alTerminar: (exito: Boolean, mensaje: String) -> Unit
    ) {
        ServicioCuentas.eliminarLabor(token, id, alTerminar)
    }
}
