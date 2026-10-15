package com.olivapp.app.simulador.domain

/** Categoría de aceite con precio de referencia (código del backend y etiqueta). */
enum class CategoriaAceite(val codigo: String, val etiqueta: String) {
    AOVE("aove", "AOVE"),
    VIRGEN("virgen", "Virgen"),
    LAMPANTE("lampante", "Lampante");

    companion object {
        fun deCodigo(codigo: String): CategoriaAceite? = entries.firstOrNull { it.codigo == codigo }
    }
}

/** Mercado español con precio de referencia (código del backend y etiqueta). */
enum class MercadoAceite(val codigo: String, val etiqueta: String) {
    MEDIA_NACIONAL("media-nacional", "Media nacional"),
    JAEN("jaen", "Jaén"),
    CORDOBA("cordoba", "Córdoba"),
    GRANADA("granada", "Granada"),
    SEVILLA("sevilla", "Sevilla"),
    MALAGA("malaga", "Málaga"),
    BADAJOZ("badajoz", "Badajoz"),
    TOLEDO("toledo", "Toledo"),
    TARRAGONA("tarragona", "Tarragona")
}

/** Una semana de precios de referencia, en €/kg de aceite, por categoría. */
data class SemanaDePrecios(
    /** Lunes de la semana en ISO (AAAA-MM-DD). */
    val semana: String,
    /** Domingo de la semana en ISO. */
    val semanaFin: String,
    val numeroSemana: Int,
    val campana: String,
    val precios: Map<CategoriaAceite, Double>
)

/**
 * Precios de referencia servidos por el backend. `desactualizado` es true
 * cuando la fuente no respondió y se sirvió la caché (con su antigüedad).
 */
data class PreciosDeReferencia(
    val semanas: List<SemanaDePrecios>,
    val fuente: String,
    val desactualizado: Boolean,
    val antiguedadHoras: Int?
)

/** Precios de referencia del aceite, siempre a través del backend (ADR 0011). */
interface PreciosAceiteRepositorio {

    /** Semanas de una campaña ("2024/2025") en un mercado, con las tres categorías. */
    fun cargarSemanas(
        token: String,
        mercado: MercadoAceite,
        campana: String,
        alTerminar: (PreciosDeReferencia?, String) -> Unit
    )

    /** La última semana disponible en un mercado, con las tres categorías. */
    fun cargarUltimaSemana(
        token: String,
        mercado: MercadoAceite,
        alTerminar: (PreciosDeReferencia?, String) -> Unit
    )
}

/** Preferencia del socio: si el simulador avanzado está desplegado (no es dato de campaña). */
interface PreferenciasSimulador {

    fun avanzadoActivado(): Boolean

    fun guardarAvanzadoActivado(activado: Boolean)
}

/**
 * Campañas que se pueden consultar, de la más reciente a la más antigua.
 * La campaña empieza en septiembre (las semanas de cambio salen ya con la
 * campaña nueva) y el historial de la fuente arranca en 2010/2011.
 */
fun campanasDisponibles(anio: Int, mes: Int): List<String> {
    val ultimoInicio = if (mes >= 9) anio else anio - 1
    return (ultimoInicio downTo PRIMERA_CAMPANA).map { "$it/${it + 1}" }
}

private const val PRIMERA_CAMPANA = 2010
