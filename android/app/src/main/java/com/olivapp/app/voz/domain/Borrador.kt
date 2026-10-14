package com.olivapp.app.voz.domain

import org.json.JSONObject

/** Campos obligatorios de la categoría que aún faltan en el borrador IA. */
/** Fecha concreta del borrador (AAAA-MM-DD), o vacía si todavía no la tiene. */
fun fechaIsoDeBorrador(borrador: JSONObject): String {
    val fecha = borrador.optString("fecha").trim()
    return if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(fecha)) fecha else ""
}

fun camposFaltantesDeBorrador(borrador: JSONObject): List<String> {
    val categoria = borrador.optString("categoria")
    val campos = borrador.optJSONObject("campos") ?: JSONObject()
    val obligatorios = when (categoria) {
        "tratamiento" -> listOf("producto", "plaga_a_controlar", "cantidad", "unidad")
        "riego" -> listOf("cantidad", "unidad")
        "cosecha" -> listOf("modalidad", "kilos")
        else -> emptyList()
    }
    return obligatorios.filter { campo ->
        val valor = campos.opt(campo)
        valor == null || valor.toString().isBlank()
    }
}

/** Etiqueta para el socio de un campo del borrador IA. */
fun etiquetaDeCampo(campo: String): String = when (campo) {
    "producto" -> "Producto"
    "plaga_a_controlar" -> "Plaga a controlar"
    "cantidad" -> "Cantidad"
    "unidad" -> "Unidad"
    "modalidad" -> "Modalidad"
    "kilos" -> "Kilos"
    else -> campo
}

/** Etiquetas y valores del borrador IA, en el orden de la ficha del dominio. */
fun lineasDeBorrador(borrador: JSONObject): List<Pair<String, String>> {
    val categoria = borrador.optString("categoria")
    val campos = borrador.optJSONObject("campos") ?: JSONObject()
    val lineas = mutableListOf(
        "Fecha" to fechaVisible(borrador.optString("fecha")),
        "Parcela" to borrador.optString("parcela").ifEmpty { "Sin identificar" },
        "Categoría" to when (categoria) {
            "tratamiento" -> "Tratamiento fitosanitario"
            "riego" -> "Riego"
            "cosecha" -> "Cosecha"
            else -> categoria
        }
    )
    when (categoria) {
        "tratamiento" -> {
            lineas += "Producto" to campos.optString("producto").ifEmpty { "(falta)" }
            lineas += "Plaga a controlar" to
                campos.optString("plaga_a_controlar").ifEmpty { "(falta)" }
            lineas += "Cantidad" to
                cantidadVisible(campos, "cantidad", "unidad").ifEmpty { "(falta)" }
        }
        "riego" -> lineas += "Cantidad" to
            cantidadVisible(campos, "cantidad", "unidad").ifEmpty { "(falta)" }
        "cosecha" -> {
            lineas += "Modalidad" to campos.optString("modalidad").ifEmpty { "(falta)" }
            lineas += "Kilos" to numeroVisible(campos.opt("kilos")).ifEmpty { "(falta)" }
        }
    }
    val observaciones = borrador.optString("observaciones")
    if (observaciones.isNotBlank()) lineas += "Observaciones" to observaciones
    return lineas
}

/** Fecha del borrador para el socio: 2026-01-20 se lee como 20/01/2026. */
fun fechaVisible(fecha: String): String {
    val partes = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(fecha) ?: return fecha
    val (anio, mes, dia) = partes.destructured
    return "$dia/$mes/$anio"
}

/** Números del borrador sin decimales de más (2.0 se lee como 2). */
fun numeroVisible(valor: Any?): String {
    val texto = valor?.toString() ?: ""
    return if (texto.endsWith(".0")) texto.dropLast(2) else texto
}

/** Cantidad con su unidad, según las listas cerradas del dominio. */
fun cantidadVisible(campos: JSONObject, cantidad: String, unidad: String): String {
    val numero = numeroVisible(campos.opt(cantidad))
    val etiqueta = campos.optString(unidad)
    return if (etiqueta.isBlank()) numero else "$numero $etiqueta"
}
