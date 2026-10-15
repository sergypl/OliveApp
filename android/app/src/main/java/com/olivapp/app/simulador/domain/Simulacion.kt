package com.olivapp.app.simulador.domain

/** Resultado de una simulación de liquidación en limpio (en kilos de aceite). */
class ResultadoSimulacion(
    val aceiteSegunRendimiento: Double,
    val ajustePorcentaje: Double,
    val ajusteIndustrial: Double,
    val aceiteNeto: Double
)

/**
 * Liquidación en limpio: kilos × rendimiento graso − ajuste industrial (única
 * deducción del MVP). El ajuste se expresa en porcentaje (10 % por defecto,
 * ADR 0007) y es configurable por el socio. No se guarda en ningún sitio.
 */
fun simularLiquidacion(
    kilos: Double,
    rendimiento: Double,
    ajustePorcentaje: Double = 10.0
): ResultadoSimulacion {
    val aceite = kilos * rendimiento / 100.0
    val merma = aceite * ajustePorcentaje / 100.0
    return ResultadoSimulacion(aceite, ajustePorcentaje, merma, aceite - merma)
}

/**
 * Recálculo al instante del resultado con un ajuste industrial nuevo: el aceite
 * según el rendimiento no cambia, solo la deducción que se le aplica.
 */
fun recalcularConAjuste(
    anterior: ResultadoSimulacion,
    ajustePorcentaje: Double
): ResultadoSimulacion {
    val merma = anterior.aceiteSegunRendimiento * ajustePorcentaje / 100.0
    return ResultadoSimulacion(
        anterior.aceiteSegunRendimiento,
        ajustePorcentaje,
        merma,
        anterior.aceiteSegunRendimiento - merma
    )
}

/**
 * Número tecleado por el socio: acepta coma o punto decimal ("20,5" o "20.5")
 * y los puntos de millar ("10.958"). Vacío o ilegible devuelve null.
 *
 * Un punto con tres o más cifras detrás es punto de millar (como en el
 * formato que escribe `formatearNumeroDeCampo`); con menos cifras es decimal.
 */
fun numeroDeCampo(texto: String): Double? {
    val limpio = texto.trim().replace(" ", "")
    if (limpio.isEmpty()) return null
    val normalizado = if (limpio.contains(',')) {
        limpio.replace(".", "").replace(',', '.')
    } else {
        val partes = limpio.split('.')
        if (partes.size > 1 && partes.last().length >= 3) {
            limpio.replace(".", "")
        } else {
            limpio
        }
    }
    return normalizado.toDoubleOrNull()
}

/**
 * Formato español del número mientras se teclea: agrupa los millares con
 * punto ("1.000") y deja la coma decimal tal como se escribe ("1.000,5").
 * El texto formateado vuelve a leerse con `numeroDeCampo` sin perder valor.
 *
 * Un punto con una o dos cifras detrás se toma como decimal y se escribe con
 * coma; con tres o más cifras es punto de millar (así, seguir tecleando
 * detrás de "1.000" da "10.000" y no un decimal). Lo ilegible (letras u otros
 * símbolos) se deja tal cual: no formatea ni da error.
 */
fun formatearNumeroDeCampo(texto: String): String {
    val limpio = texto.trim().replace(" ", "")
    if (limpio.isEmpty()) return ""
    if (!limpio.all { it.isDigit() || it == ',' || it == '.' }) return limpio
    val coma = limpio.lastIndexOf(',')
    val entero: String
    val decimales: String
    if (coma >= 0) {
        entero = limpio.substring(0, coma).filter { it.isDigit() }
        decimales = limpio.substring(coma + 1).filter { it.isDigit() }
    } else {
        val punto = limpio.lastIndexOf('.')
        if (punto >= 0 && limpio.substring(punto + 1).length in 0..2) {
            // Punto decimal tecleado: se escribe como coma.
            entero = limpio.substring(0, punto).filter { it.isDigit() }
            decimales = limpio.substring(punto + 1).filter { it.isDigit() }
        } else {
            entero = limpio.filter { it.isDigit() }
            decimales = ""
        }
    }
    val conCero = entero.trimStart('0').ifEmpty { if (entero.isEmpty()) "" else "0" }
    val agrupado = conCero.reversed().chunked(3).joinToString(".").reversed()
    return if (decimales.isEmpty() && !limpio.endsWith(',') && !limpio.endsWith('.')) {
        agrupado
    } else if (agrupado.isEmpty()) {
        "0,$decimales"
    } else {
        "$agrupado,$decimales"
    }
}
