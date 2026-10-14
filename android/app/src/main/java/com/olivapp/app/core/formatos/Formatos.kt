package com.olivapp.app.core.formatos

import com.olivapp.app.cuaderno.domain.DatosLabor
import com.olivapp.app.cuaderno.domain.LaborEditable

/** Números en formato español: 10.958 y 17,93. */
internal fun formatearNumero(valor: Double, decimales: Int): String {
    val formato = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-ES"))
    formato.minimumFractionDigits = decimales
    formato.maximumFractionDigits = decimales
    return formato.format(valor)
}

/** Cantidades con los decimales justos: 3, 2,5 y 850. */
internal fun formatearCantidad(valor: Double): String {
    val formato = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-ES"))
    formato.minimumFractionDigits = 0
    formato.maximumFractionDigits = 2
    return formato.format(valor)
}

/** Fecha ISO (AAAA-MM-DD) a formato español (DD/MM/AAAA). */
internal fun formatearFecha(iso: String): String {
    val partes = iso.split("-")
    if (partes.size != 3) return iso
    return partes[2] + "/" + partes[1] + "/" + partes[0]
}

internal val NOMBRES_DE_CATEGORIA = mapOf(
    "tratamiento" to "Tratamiento fitosanitario",
    "riego" to "Riego",
    "cosecha" to "Cosecha"
)

/** Una labor del backend a texto para "Mi cuaderno". */
internal fun formatearLabor(labor: org.json.JSONObject): DatosLabor {
    val campos = labor.optJSONObject("campos") ?: org.json.JSONObject()
    val parcela = labor.optJSONObject("parcela")?.optString("nombre") ?: ""
    val parejas = when (labor.optString("categoria")) {
        "tratamiento" -> listOf(
            "Producto" to campos.optString("producto"),
            "Plaga a controlar" to campos.optString("plaga_a_controlar"),
            "Cantidad" to formatearCantidad(campos.optDouble("cantidad")) +
                " " + campos.optString("unidad")
        )
        "riego" -> listOf(
            "Cantidad" to formatearCantidad(campos.optDouble("cantidad")) +
                " " + campos.optString("unidad")
        )
        "cosecha" -> listOf(
            "Modalidad" to campos.optString("modalidad"),
            "Kilos" to formatearNumero(campos.optDouble("kilos"), 0) + " kg"
        )
        else -> emptyList()
    }
    return DatosLabor(
        fecha = formatearFecha(labor.optString("fecha")),
        categoria = NOMBRES_DE_CATEGORIA[labor.optString("categoria")]
            ?: labor.optString("categoria"),
        parcela = parcela,
        campos = parejas,
        observaciones = labor.optString("observaciones"),
        editable = LaborEditable(
            id = labor.optString("id"),
            fechaVisible = formatearFecha(labor.optString("fecha")),
            fechaIso = labor.optString("fecha"),
            parcelaId = labor.optJSONObject("parcela")?.optString("id") ?: "",
            parcelaNombre = parcela,
            categoria = labor.optString("categoria"),
            campos = campos,
            observaciones = labor.optString("observaciones")
        )
    )
}

/**
 * Variación porcentual entre la cifra actual y la anterior (tarjeta de dato de
 * core/ui). Sin comparación posible (valores a 0 o sin cifra anterior) se
 * devuelve null y no se muestra la variación.
 */
internal fun variacionPorcentual(actual: Double, anterior: Double): Double? {
    if (actual <= 0.0 || anterior <= 0.0) return null
    return (actual - anterior) / anterior * 100.0
}

/** Variación a texto con signo y formato español: "+48 %", "−12,5 %" o "0 %". */
internal fun textoVariacion(porcentaje: Double): String {
    val redondeado = kotlin.math.round(porcentaje * 10.0) / 10.0
    val signo = when {
        redondeado > 0.0 -> "+"
        redondeado < 0.0 -> "−"
        else -> ""
    }
    val decimales = if (redondeado % 1.0 == 0.0) 0 else 1
    return signo + formatearNumero(kotlin.math.abs(redondeado), decimales) + " %"
}
