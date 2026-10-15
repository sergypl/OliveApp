package com.olivapp.app.campana.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.DatosPanel
import com.olivapp.app.perfil.presentation.FakePerfilRepositorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests del ViewModel del panel (features panel-de-campana y panel-moderno). */
class PanelViewModelTest {

    private val sesion = FakeSesionRepositorio().apply { guardar("token-abc") }
    private val campanas = FakeCampanasRepositorio()
    private val perfil = FakePerfilRepositorio()

    private fun viewModel(sesion: FakeSesionRepositorio = this.sesion) =
        PanelViewModel(sesion, campanas, perfil)

    @Test
    fun `con sesion abierta se cargan los datos oficiales y las campanas`() {
        val viewModel = viewModel()

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("token-abc", campanas.tokenLlamada)
        assertEquals("2025/2026", estado.datos!!.campana)
        assertEquals("10.958 kg", estado.datos!!.kilosEntregados)
        assertEquals("17,93 %", estado.datos!!.rendimientoGraso)
        assertEquals("1.774 kg", estado.datos!!.aceiteObtenido)
        assertEquals(listOf("c-2024"), estado.campanas.map { it.id })
        assertFalse(estado.cargando)
    }

    @Test
    fun `la cabecera usa el perfil del socio`() {
        val viewModel = viewModel()

        viewModel.cargar()

        val cargado = viewModel.uiState.value.perfil!!
        assertEquals("token-abc", perfil.tokenLlamada)
        assertEquals("Socio 1", cargado.codigoSocio)
        assertEquals("Cooperativa Olivarera de Demostración", cargado.cooperativa)
    }

    @Test
    fun `si falla el perfil el panel carga igual y la cabecera se degrada`() {
        perfil.resultado = null to "No hay conexión con el servidor. Inténtalo de nuevo."
        val viewModel = viewModel()

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertNull(estado.perfil)
        assertNotNull(estado.datos)
        assertNull(estado.mensaje)
        assertFalse(estado.cargando)
    }

    @Test
    fun `sin token se avisa de que la sesion ha caducado`() {
        val viewModel = viewModel(FakeSesionRepositorio())

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
        assertEquals(null, estado.datos)
        assertEquals(null, campanas.tokenLlamada)
        assertFalse(estado.cargando)
    }

    @Test
    fun `si fallan las campanas se ensena el mensaje del backend sin tocar los datos`() {
        campanas.resultadoCampanas = null to "No se pudieron cargar tus campañas."
        val viewModel = viewModel()

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("No se pudieron cargar tus campañas.", estado.errorDeCampanas)
        assertNull(estado.mensaje)
        assertNotNull(estado.datos)
    }

    @Test
    fun `un fallo de red al cargar los datos no parece que no haya datos`() {
        campanas.resultadoMiPanel = null to "No hay conexión con el servidor. Inténtalo de nuevo."
        val viewModel = viewModel()

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertNull(estado.datos)
        assertEquals("No hay conexión con el servidor. Inténtalo de nuevo.", estado.mensaje)
        assertTrue(estado.sinConexion)
        assertFalse(estado.cargando)
    }

    @Test
    fun `reintentar tras un fallo carga los datos y limpia el mensaje`() {
        campanas.resultadoMiPanel = null to "No hay conexión con el servidor. Inténtalo de nuevo."
        val viewModel = viewModel()
        viewModel.cargar()
        campanas.resultadoMiPanel = DatosPanel("2025/2026", "10.958 kg", "17,93 %", "1.774 kg") to ""

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertNull(estado.mensaje)
        assertEquals("2025/2026", estado.datos!!.campana)
    }

    @Test
    fun `mientras carga por primera vez hay esqueleto y al terminar desaparece`() {
        campanas.diferido = true
        val viewModel = viewModel()

        viewModel.cargar()
        assertTrue(viewModel.uiState.value.cargando)
        assertNull(viewModel.uiState.value.datos)

        campanas.terminarPendientes()
        assertFalse(viewModel.uiState.value.cargando)
        assertNotNull(viewModel.uiState.value.datos)
    }

    @Test
    fun `refrescar enseña el indicador sin esqueleto y conserva los datos si falla`() {
        val viewModel = viewModel()
        viewModel.cargar()
        campanas.diferido = true

        viewModel.alRefrescar()
        assertTrue(viewModel.uiState.value.refrescando)
        assertFalse(viewModel.uiState.value.cargando)
        assertNotNull(viewModel.uiState.value.datos)

        campanas.resultadoMiPanel = null to "No hay conexión con el servidor. Inténtalo de nuevo."
        campanas.terminarPendientes()
        val estado = viewModel.uiState.value
        assertFalse(estado.refrescando)
        assertNotNull(estado.datos)
        assertTrue(estado.sinConexion)
    }

    @Test
    fun `si hay una carga en curso no se lanza otra`() {
        campanas.diferido = true
        val viewModel = viewModel()
        viewModel.cargar()

        viewModel.cargar()
        viewModel.alRefrescar()

        assertEquals(1, campanas.llamadasPanel)
        assertFalse(viewModel.uiState.value.refrescando)
    }

    @Test
    fun `la comparativa sale de las campanas con su aceite`() {
        campanas.resultadoCampanas = listOf(
            DatosCampanaResumen("2025-2026", "2025/2026", "actual", 1774.0),
            DatosCampanaResumen("2024-2025", "2024/2025", "cerrada", 1198.0)
        ) to ""
        val viewModel = viewModel()

        viewModel.cargar()

        val estado = viewModel.uiState.value
        assertEquals("+48 %", estado.comparativa!!.textoVariacion)
        assertEquals(2, estado.barras.size)
        assertEquals(listOf("2024-2025"), estado.campanasAnteriores.map { it.id })
    }
}
