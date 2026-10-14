package com.olivapp.app.cuaderno.domain

/** Una labor de "Mi cuaderno", ya formateada para enseñar. */
class DatosLabor(
    val fecha: String,
    val categoria: String,
    val parcela: String,
    val campos: List<Pair<String, String>>,
    val observaciones: String,
    val editable: LaborEditable
)

/** Los datos de una labor tal cual para poder editarla ('Corrección manual'). */
class LaborEditable(
    val id: String,
    val fechaVisible: String,
    val fechaIso: String,
    val parcelaId: String,
    val parcelaNombre: String,
    val categoria: String,
    val campos: org.json.JSONObject,
    val observaciones: String
)

/** Un grupo de "Mi cuaderno": una campaña con sus labores. */
class DatosCampanaDeLabores(
    val nombre: String,
    val esActual: Boolean,
    val labores: List<DatosLabor>
)
