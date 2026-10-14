package com.olivapp.app.campana.domain

/**
 * Campañas y datos oficiales de la cooperativa para el socio. Responde siempre
 * en el hilo principal (vía el gateway HTTP).
 */
interface CampanasRepositorio {

    /** Datos oficiales de la campaña actual (panel). */
    fun cargarMiPanel(token: String, alTerminar: (datos: DatosPanel?, mensaje: String) -> Unit)

    /** Campañas del socio (abiertas y cerradas). */
    fun cargarCampanas(
        token: String,
        alTerminar: (lista: List<DatosCampanaResumen>?, mensaje: String) -> Unit
    )

    /** Detalle en consulta de una campaña concreta. */
    fun cargarCampana(
        token: String,
        campanaId: String,
        alTerminar: (cargada: DatosCampana?, mensaje: String) -> Unit
    )
}
