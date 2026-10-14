package com.olivapp.app.campana.presentation

import com.olivapp.app.campana.domain.CampanasRepositorio
import com.olivapp.app.campana.domain.DatosCampana
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.DatosPanel

/**
 * Fake del repositorio de campañas para testear los ViewModels sin red ni
 * Hilt: responde al momento con resultados configurables.
 */
class FakeCampanasRepositorio : CampanasRepositorio {

    var resultadoMiPanel: Pair<DatosPanel?, String> = DatosPanel(
        campana = "2025/2026",
        kilosEntregados = "10.958 kg",
        rendimientoGraso = "17,93 %",
        aceiteObtenido = "1.774 kg"
    ) to ""

    var resultadoCampanas: Pair<List<DatosCampanaResumen>?, String> = listOf(
        DatosCampanaResumen(id = "c-2024", nombre = "2024/2025", estado = "cerrada")
    ) to ""

    var resultadoCampana: Pair<DatosCampana?, String> = DatosCampana(
        nombre = "2024/2025",
        estado = "cerrada",
        kilosEntregados = "8.000 kg",
        rendimientoGraso = "18,5 %",
        aceiteObtenido = "1.480 kg",
        labores = emptyList()
    ) to ""

    var tokenLlamada: String? = null
    var campanaIdLlamada: String? = null

    override fun cargarMiPanel(token: String, alTerminar: (DatosPanel?, String) -> Unit) {
        tokenLlamada = token
        alTerminar(resultadoMiPanel.first, resultadoMiPanel.second)
    }

    override fun cargarCampanas(
        token: String,
        alTerminar: (List<DatosCampanaResumen>?, String) -> Unit
    ) {
        tokenLlamada = token
        alTerminar(resultadoCampanas.first, resultadoCampanas.second)
    }

    override fun cargarCampana(
        token: String,
        campanaId: String,
        alTerminar: (DatosCampana?, String) -> Unit
    ) {
        tokenLlamada = token
        campanaIdLlamada = campanaId
        alTerminar(resultadoCampana.first, resultadoCampana.second)
    }
}
