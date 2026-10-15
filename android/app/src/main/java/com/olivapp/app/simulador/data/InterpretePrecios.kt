package com.olivapp.app.simulador.data

import com.olivapp.app.simulador.domain.CategoriaAceite
import com.olivapp.app.simulador.domain.PreciosDeReferencia
import com.olivapp.app.simulador.domain.SemanaDePrecios
import org.json.JSONObject

/** Lee las respuestas de `/api/v1/precios-aceite` del backend (precios ya en €/kg). */
internal object InterpretePrecios {

    /** Respuesta de `GET /api/v1/precios-aceite`: las semanas de una campaña. */
    fun semanas(json: JSONObject): PreciosDeReferencia {
        val arreglo = json.optJSONArray("semanas")
        val semanas = mutableListOf<SemanaDePrecios>()
        if (arreglo != null) {
            for (i in 0 until arreglo.length()) {
                val semana = arreglo.optJSONObject(i)?.let { semana(it) } ?: continue
                semanas.add(semana)
            }
        }
        return referencia(json, semanas)
    }

    /** Respuesta de `GET /api/v1/precios-aceite/ultima-semana`: una sola semana. */
    fun ultimaSemana(json: JSONObject): PreciosDeReferencia? {
        val semana = semana(json) ?: return null
        return referencia(json, listOf(semana))
    }

    private fun semana(json: JSONObject): SemanaDePrecios? {
        val fecha = json.optString("semana")
        val preciosJson = json.optJSONObject("precios") ?: return null
        val precios = mutableMapOf<CategoriaAceite, Double>()
        for (categoria in CategoriaAceite.entries) {
            if (!preciosJson.has(categoria.codigo) || preciosJson.isNull(categoria.codigo)) continue
            val valor = preciosJson.optDouble(categoria.codigo, Double.NaN)
            if (!valor.isNaN()) precios[categoria] = valor
        }
        if (fecha.isEmpty() || precios.isEmpty()) return null
        return SemanaDePrecios(
            semana = fecha,
            semanaFin = json.optString("semana_fin", fecha),
            numeroSemana = json.optInt("numero_semana"),
            campana = json.optString("campana"),
            precios = precios
        )
    }

    private fun referencia(json: JSONObject, semanas: List<SemanaDePrecios>) =
        PreciosDeReferencia(
            semanas = semanas,
            fuente = json.optString("fuente", FUENTE),
            desactualizado = json.optBoolean("desactualizado", false),
            antiguedadHoras = if (json.has("antiguedad_horas") && !json.isNull("antiguedad_horas")) {
                json.optInt("antiguedad_horas")
            } else {
                null
            }
        )

    const val FUENTE = "Comisión Europea · Agri-food Data Portal"
}
