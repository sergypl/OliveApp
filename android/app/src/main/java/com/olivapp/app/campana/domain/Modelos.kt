package com.olivapp.app.campana.domain

import com.olivapp.app.cuaderno.domain.DatosLabor

/** Datos oficiales del panel, ya formateados para enseñar (formato español). */
class DatosPanel(
    val campana: String,
    val kilosEntregados: String,
    val rendimientoGraso: String,
    val aceiteObtenido: String
)

/**
 * Resumen de una campaña en el listado del panel. [aceiteKg] es el aceite
 * obtenido oficial en kg (sin formatear, para la comparativa); `null` si el
 * backend no lo envía.
 */
class DatosCampanaResumen(
    val id: String,
    val nombre: String,
    val estado: String,
    val aceiteKg: Double? = null
)

/** Detalle de una campaña en modo consulta (labores aparte de lo oficial). */
class DatosCampana(
    val nombre: String,
    val estado: String,
    val kilosEntregados: String,
    val rendimientoGraso: String,
    val aceiteObtenido: String,
    val labores: List<DatosLabor>
)
