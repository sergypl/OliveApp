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
 */
fun numeroDeCampo(texto: String): Double? {
    val limpio = texto.trim().replace(" ", "")
    if (limpio.isEmpty()) return null
    val normalizado = if (limpio.contains(',')) {
        limpio.replace(".", "").replace(',', '.')
    } else {
        val partes = limpio.split('.')
        if (partes.size > 1 && partes.last().length == 3) {
            limpio.replace(".", "")
        } else {
            limpio
        }
    }
    return normalizado.toDoubleOrNull()
}
