package com.olivapp.app.cuaderno.domain

/**
 * Lógica pura de "Mi cuaderno" como línea de tiempo (feature
 * cuaderno-linea-de-tiempo): filtros por categoría y parcela y agrupación de
 * las labores por mes dentro de cada campaña.
 *
 * Solo datos, sin Android ni red: la pantalla solo dibuja lo que sale de aquí
 * y los tests la cubren directamente. El orden siempre es de lo más reciente a
 * lo más antiguo, igual que el cuaderno de campo.
 */

/** Filtro de la línea de tiempo. Un campo a `null` significa "todas". */
data class FiltroDeCuaderno(
    val categoria: String? = null,
    val parcela: String? = null
) {
    /** `true` cuando no se está filtrando nada. */
    val sinFiltros: Boolean
        get() = categoria == null && parcela == null
}

/** Un mes de la línea de tiempo con sus labores, de la más reciente a la más antigua. */
data class GrupoDeMes(
    /** Clave del mes, por ejemplo "2025-12" (para ordenar y para las cabeceras fijas). */
    val clave: String,
    /** Título para enseñar, por ejemplo "Diciembre de 2025". */
    val titulo: String,
    val labores: List<DatosLabor>
)

/** Una campaña de la línea de tiempo con sus meses, del más reciente al más antiguo. */
data class CampanaDeLineaDeTiempo(
    val nombre: String,
    val esActual: Boolean,
    val meses: List<GrupoDeMes>
)

/** Opciones de los chips de filtro, tomadas de las labores que hay cargadas. */
data class OpcionesDeFiltro(
    val categorias: List<String>,
    val parcelas: List<String>
)

/** Orden de las categorías en los chips de filtro (ADR 0005). */
private val ORDEN_DE_CATEGORIAS = listOf("tratamiento", "riego", "cosecha")

private val MESES_DEL_ANO = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/** Quita las labores que no encajan con el filtro y las campañas que se quedan vacías. */
fun filtrarCampanas(
    campanas: List<DatosCampanaDeLabores>,
    filtro: FiltroDeCuaderno
): List<DatosCampanaDeLabores> = campanas
    .map { campana ->
        DatosCampanaDeLabores(
            nombre = campana.nombre,
            esActual = campana.esActual,
            labores = campana.labores.filter { labor ->
                val deCategoria =
                    filtro.categoria == null ||
                        labor.editable.categoria.equals(filtro.categoria, ignoreCase = true)
                val deParcela = filtro.parcela == null || labor.parcela == filtro.parcela
                deCategoria && deParcela
            }
        )
    }
    .filter { it.labores.isNotEmpty() }

/**
 * Agrupa las labores por mes dentro de cada campaña, para la línea de tiempo.
 * Las cabeceras de mes se ordenan de más reciente a más antigua y cada mes
 * conserva ese mismo orden interno.
 */
fun agruparPorMes(campanas: List<DatosCampanaDeLabores>): List<CampanaDeLineaDeTiempo> =
    campanas.map { campana ->
        val porMes = campana.labores
            .sortedByDescending { it.editable.fechaIso }
            .groupBy { claveDeMes(it.editable.fechaIso) }
        CampanaDeLineaDeTiempo(
            nombre = campana.nombre,
            esActual = campana.esActual,
            meses = porMes.entries
                .sortedByDescending { it.key }
                .map { (clave, labores) -> GrupoDeMes(clave, tituloDeMes(clave), labores) }
        )
    }

/** Categorías y parcelas presentes en el cuaderno, en orden estable para los chips. */
fun opcionesDeFiltro(campanas: List<DatosCampanaDeLabores>): OpcionesDeFiltro {
    val labores = campanas.flatMap { it.labores }
    val categorias = labores.map { it.editable.categoria.lowercase() }.distinct()
    return OpcionesDeFiltro(
        categorias = ORDEN_DE_CATEGORIAS.filter { it in categorias },
        parcelas = labores.map { it.parcela }.filter { it.isNotBlank() }.distinct().sorted()
    )
}

/** Clave del mes de una fecha ISO (AAAA-MM-DD): "2025-12". */
fun claveDeMes(fechaIso: String): String = fechaIso.take(7)

/** Título de una cabecera de mes: "2025-12" → "Diciembre de 2025". */
fun tituloDeMes(clave: String): String {
    val partes = clave.split("-")
    if (partes.size < 2) return "Sin fecha"
    val mes = partes[1].toIntOrNull()
    val ano = partes[0].toIntOrNull()
    if (mes == null || ano == null || mes < 1 || mes > 12) return "Sin fecha"
    return MESES_DEL_ANO[mes - 1] + " de " + ano
}
