package com.olivapp.app.cuaderno.presentation

import com.olivapp.app.cuaderno.domain.DatosCampanaDeLabores
import com.olivapp.app.cuaderno.domain.DatosLabor
import com.olivapp.app.cuaderno.domain.LaboresRepositorio
import com.olivapp.app.cuaderno.domain.LaborEditable
import org.json.JSONObject

/**
 * Fake del repositorio de labores para testear los ViewModels sin red ni Hilt:
 * responde al momento o retiene la llamada para simular espera, y guarda el
 * cuerpo enviado para comprobar qué se pide al backend.
 */
class FakeLaboresRepositorio(
    private val responderAlMomento: Boolean = true
) : LaboresRepositorio {

    var resultadoMisLabores: Pair<List<DatosCampanaDeLabores>?, String> = listOf(
        DatosCampanaDeLabores(nombre = "2025/2026", esActual = true, labores = emptyList())
    ) to ""

    var resultadoGuardar: Pair<String?, String> = "2025/2026" to "Labor guardada en la campaña 2025/2026."
    var resultadoActualizar: Pair<Boolean, String> = true to "Cambios guardados en tu cuaderno."
    var resultadoEliminar: Pair<Boolean, String> = true to "Labor eliminada de tu cuaderno."

    var tokenLlamada: String? = null
    var idLlamado: String? = null
    var cuerpoLlamado: JSONObject? = null

    var pendienteActualizar: ((Boolean, String) -> Unit)? = null

    override fun cargarMisLabores(
        token: String,
        alTerminar: (lista: List<DatosCampanaDeLabores>?, mensaje: String) -> Unit
    ) {
        tokenLlamada = token
        alTerminar(resultadoMisLabores.first, resultadoMisLabores.second)
    }

    override fun guardarLabor(
        token: String,
        cuerpo: JSONObject,
        alTerminar: (campana: String?, mensaje: String) -> Unit
    ) {
        tokenLlamada = token
        cuerpoLlamado = cuerpo
        alTerminar(resultadoGuardar.first, resultadoGuardar.second)
    }

    override fun actualizarLabor(
        token: String,
        id: String,
        cuerpo: JSONObject,
        alTerminar: (exito: Boolean, mensaje: String) -> Unit
    ) {
        tokenLlamada = token
        idLlamado = id
        cuerpoLlamado = cuerpo
        if (responderAlMomento) {
            alTerminar(resultadoActualizar.first, resultadoActualizar.second)
        } else {
            pendienteActualizar = alTerminar
        }
    }

    override fun eliminarLabor(
        token: String,
        id: String,
        alTerminar: (exito: Boolean, mensaje: String) -> Unit
    ) {
        tokenLlamada = token
        idLlamado = id
        alTerminar(resultadoEliminar.first, resultadoEliminar.second)
    }
}

/** Una labor de ejemplo para los tests de edición. */
fun laborDeEjemplo() = LaborEditable(
    id = "lab-7",
    fechaVisible = "15/12/2025",
    fechaIso = "2025-12-15",
    parcelaId = "p-1",
    parcelaNombre = "Cañada Honda",
    categoria = "tratamiento",
    campos = JSONObject()
        .put("producto", "Recinol")
        .put("plaga_a_controlar", "repilo")
        .put("cantidad", 2.5)
        .put("unidad", "L"),
    observaciones = "Con viento flojo"
)

/** Una labor ya formateada para enseñar (para armar listas de ejemplo). */
fun datosLaborDeEjemplo() = DatosLabor(
    fecha = "15/12/2025 · tratamiento",
    categoria = "tratamiento",
    parcela = "Cañada Honda",
    campos = listOf("Producto" to "Recinol"),
    observaciones = "",
    editable = laborDeEjemplo()
)
