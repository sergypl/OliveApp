package com.olivapp.app.campana.data

import com.olivapp.app.campana.domain.CampanasRepositorio
import com.olivapp.app.campana.domain.DatosCampana
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.DatosPanel
import com.olivapp.app.core.red.ServicioCuentas

/**
 * Campañas del socio contra el backend: delega en el gateway HTTP
 * (`ServicioCuentas`), que responde siempre en el hilo principal.
 */
class CampanasRepositorioImpl : CampanasRepositorio {

    override fun cargarMiPanel(
        token: String,
        alTerminar: (datos: DatosPanel?, mensaje: String) -> Unit
    ) {
        ServicioCuentas.cargarMiPanel(token, alTerminar)
    }

    override fun cargarCampanas(
        token: String,
        alTerminar: (lista: List<DatosCampanaResumen>?, mensaje: String) -> Unit
    ) {
        ServicioCuentas.cargarCampanas(token, alTerminar)
    }

    override fun cargarCampana(
        token: String,
        campanaId: String,
        alTerminar: (cargada: DatosCampana?, mensaje: String) -> Unit
    ) {
        ServicioCuentas.cargarCampana(token, campanaId, alTerminar)
    }
}
