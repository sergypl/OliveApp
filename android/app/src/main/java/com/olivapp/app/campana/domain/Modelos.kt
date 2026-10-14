package com.olivapp.app.campana.domain

import com.olivapp.app.DatosLabor

/** Datos oficiales del panel, ya formateados para enseñar (formato español). */
class DatosPanel(
    val campana: String,
    val kilosEntregados: String,
    val rendimientoGraso: String,
    val aceiteObtenido: String
)

/** Resumen de una campaña en el listado del panel. */
class DatosCampanaResumen(val id: String, val nombre: String, val estado: String)

/** Detalle de una campaña en modo consulta (labores aparte de lo oficial). */
class DatosCampana(
    val nombre: String,
    val estado: String,
    val kilosEntregados: String,
    val rendimientoGraso: String,
    val aceiteObtenido: String,
    val labores: List<DatosLabor>
)
