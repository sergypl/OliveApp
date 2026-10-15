package com.olivapp.app.simulador.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import com.olivapp.app.simulador.domain.CategoriaAceite
import com.olivapp.app.simulador.domain.MercadoAceite
import com.olivapp.app.simulador.domain.PreciosAceiteRepositorio
import com.olivapp.app.simulador.domain.PreciosDeReferencia
import com.olivapp.app.simulador.domain.PreferenciasSimulador
import com.olivapp.app.simulador.domain.SemanaDePrecios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del ViewModel del simulador avanzado (feature simulador-avanzado-precio-aceite).
 * Puro JVM: el repositorio de precios y las preferencias son falsos.
 */
class SimuladorAvanzadoViewModelTest {

    private val delta = 1e-9

    private fun semana(lunes: String, campana: String, aove: Double, virgen: Double, lampante: Double) =
        SemanaDePrecios(
            semana = lunes,
            semanaFin = lunes,
            numeroSemana = 1,
            campana = campana,
            precios = mapOf(
                CategoriaAceite.AOVE to aove,
                CategoriaAceite.VIRGEN to virgen,
                CategoriaAceite.LAMPANTE to lampante
            )
        )

    private val ultima = semana("2026-10-05", "2026/2027", 4.0, 3.5, 3.0)
    private val anterior = semana("2026-09-28", "2026/2027", 3.9, 3.4, 2.9)

    private class FakePrecios : PreciosAceiteRepositorio {
        var ultimaSemana: Pair<PreciosDeReferencia?, String> = null to "sin configurar"
        var semanas: Pair<PreciosDeReferencia?, String> = null to "sin configurar"
        var llamadasUltima = 0
        val llamadasSemanas = mutableListOf<Triple<String, MercadoAceite, String>>()

        override fun cargarSemanas(
            token: String,
            mercado: MercadoAceite,
            campana: String,
            alTerminar: (PreciosDeReferencia?, String) -> Unit
        ) {
            llamadasSemanas.add(Triple(token, mercado, campana))
            alTerminar(semanas.first, semanas.second)
        }

        override fun cargarUltimaSemana(
            token: String,
            mercado: MercadoAceite,
            alTerminar: (PreciosDeReferencia?, String) -> Unit
        ) {
            llamadasUltima++
            alTerminar(ultimaSemana.first, ultimaSemana.second)
        }
    }

    private class FakePreferencias(var activado: Boolean = false) : PreferenciasSimulador {
        override fun avanzadoActivado() = activado
        override fun guardarAvanzadoActivado(activado: Boolean) {
            this.activado = activado
        }
    }

    private fun referencia(vararg semanas: SemanaDePrecios, desactualizado: Boolean = false) =
        PreciosDeReferencia(semanas.toList(), "Fuente", desactualizado, if (desactualizado) 5 else 0)

    private fun nuevo(
        precios: FakePrecios = FakePrecios(),
        preferencias: FakePreferencias = FakePreferencias(),
        conSesion: Boolean = true
    ): SimuladorAvanzadoViewModel {
        val sesion = FakeSesionRepositorio()
        if (conSesion) sesion.guardar("token-abc")
        return SimuladorAvanzadoViewModel(sesion, precios, preferencias)
    }

    private fun conPrecios(): Pair<SimuladorAvanzadoViewModel, FakePrecios> {
        val precios = FakePrecios()
        precios.ultimaSemana = referencia(ultima) to ""
        precios.semanas = referencia(ultima, anterior) to ""
        return nuevo(precios) to precios
    }

    @Test
    fun `arranca desactivado y sin pedir precios`() {
        val precios = FakePrecios()
        val viewModel = nuevo(precios)

        assertFalse(viewModel.uiState.value.activado)
        assertEquals(0, precios.llamadasUltima)
        assertEquals("0,20", viewModel.uiState.value.gastos)
        assertEquals("100", viewModel.uiState.value.aove)
    }

    @Test
    fun `activar guarda la preferencia y elige la ultima semana disponible`() {
        val (_, precios) = conPrecios()
        val preferencias = FakePreferencias()
        val conPreferencias = SimuladorAvanzadoViewModel(
            FakeSesionRepositorio().apply { guardar("token-abc") }, precios, preferencias
        )

        conPreferencias.activar(true)

        assertTrue(preferencias.activado)
        val estado = conPreferencias.uiState.value
        assertTrue(estado.activado)
        assertEquals("2026-10-05", estado.semanaElegida)
        assertEquals("2026/2027", estado.campana)
        // Tras la última semana se carga la lista de la campaña.
        assertEquals(listOf(Triple("token-abc", MercadoAceite.MEDIA_NACIONAL, "2026/2027")),
            precios.llamadasSemanas)
        assertEquals(2, estado.semanas.size)
        assertEquals(4.0, estado.precio!!, delta)
    }

    @Test
    fun `con la preferencia guardada arranca activado y carga el precio`() {
        val precios = FakePrecios()
        precios.ultimaSemana = referencia(ultima) to ""
        precios.semanas = referencia(ultima) to ""

        val viewModel = nuevo(precios, FakePreferencias(activado = true))

        assertTrue(viewModel.uiState.value.activado)
        assertEquals(4.0, viewModel.uiState.value.precio!!, delta)
    }

    @Test
    fun `cambiar de categoria cambia el precio y reparte el 100 por cien`() {
        val (viewModel, _) = conPrecios()
        viewModel.activar(true)

        viewModel.cambiarCategoria(CategoriaAceite.LAMPANTE)

        val estado = viewModel.uiState.value
        assertEquals("0", estado.aove)
        assertEquals("100", estado.lampante)
        assertEquals(3.0, estado.precio!!, delta)
    }

    @Test
    fun `el reparto por calidad pondera con los precios de la misma semana`() {
        val (viewModel, _) = conPrecios()
        viewModel.activar(true)

        viewModel.cambiarReparto(CategoriaAceite.AOVE, "60")
        viewModel.cambiarReparto(CategoriaAceite.VIRGEN, "30")
        viewModel.cambiarReparto(CategoriaAceite.LAMPANTE, "10")

        val estado = viewModel.uiState.value
        assertEquals(3.75, estado.precio!!, delta)
        assertEquals("", estado.avisoDeReparto)
    }

    @Test
    fun `un reparto que no suma 100 avisa y no da precio`() {
        val (viewModel, _) = conPrecios()
        viewModel.activar(true)

        viewModel.cambiarReparto(CategoriaAceite.AOVE, "60")

        val estado = viewModel.uiState.value
        assertNull(estado.precio)
        assertEquals("El reparto suma 60 %. Tiene que sumar 100 %.", estado.avisoDeReparto)
    }

    @Test
    fun `elegir otra semana de la lista cambia el precio`() {
        val (viewModel, _) = conPrecios()
        viewModel.activar(true)

        viewModel.elegirSemana("2026-09-28")

        assertEquals(3.9, viewModel.uiState.value.precio!!, delta)
    }

    @Test
    fun `cambiar de mercado o de campana recarga la lista y elige su semana mas reciente`() {
        val (viewModel, precios) = conPrecios()
        viewModel.activar(true)
        precios.semanas = referencia(anterior) to ""

        viewModel.cambiarMercado(MercadoAceite.JAEN)
        assertEquals(MercadoAceite.JAEN, precios.llamadasSemanas.last().second)
        assertEquals("2026-09-28", viewModel.uiState.value.semanaElegida)

        viewModel.cambiarCampana("2024/2025")
        assertEquals("2024/2025", precios.llamadasSemanas.last().third)
    }

    @Test
    fun `con precio escrito a mano se aplica el precio unico y el reparto se oculta`() {
        val (viewModel, _) = conPrecios()
        viewModel.activar(true)
        assertTrue(viewModel.uiState.value.repartoVisible)

        viewModel.cambiarModo(ModoPrecio.MANUAL)
        viewModel.cambiarPrecioManual("4,45")
        // Un reparto roto no afecta al precio manual.
        viewModel.cambiarReparto(CategoriaAceite.AOVE, "10")

        val estado = viewModel.uiState.value
        assertFalse(estado.repartoVisible)
        assertEquals(4.45, estado.precio!!, delta)
        assertEquals("", estado.avisoDeReparto)
    }

    @Test
    fun `el precio manual vacio o ilegible no da precio`() {
        val (viewModel, _) = conPrecios()
        viewModel.activar(true)
        viewModel.cambiarModo(ModoPrecio.MANUAL)

        assertNull(viewModel.uiState.value.precio)
        viewModel.cambiarPrecioManual("abc")
        assertNull(viewModel.uiState.value.precio)
    }

    @Test
    fun `los gastos se editan con coma decimal`() {
        val (viewModel, _) = conPrecios()

        viewModel.cambiarGastos("0,35")

        assertEquals(0.35, viewModel.uiState.value.gastosPorKilo!!, delta)
        viewModel.cambiarGastos("")
        assertNull(viewModel.uiState.value.gastosPorKilo)
    }

    @Test
    fun `si falla la carga se avisa y el precio a mano sigue funcionando`() {
        val precios = FakePrecios()
        precios.ultimaSemana = null to "No hay conexión con el servidor. Puedes escribir el precio a mano."
        val viewModel = nuevo(precios)

        viewModel.activar(true)

        var estado = viewModel.uiState.value
        assertEquals("No hay conexión con el servidor. Puedes escribir el precio a mano.", estado.aviso)
        assertFalse(estado.cargando)
        assertNull(estado.precio)

        viewModel.cambiarModo(ModoPrecio.MANUAL)
        viewModel.cambiarPrecioManual("4")
        estado = viewModel.uiState.value
        assertEquals(4.0, estado.precio!!, delta)
    }

    @Test
    fun `si la lista falla tras la ultima semana se conserva la semana elegida`() {
        val precios = FakePrecios()
        precios.ultimaSemana = referencia(ultima) to ""
        precios.semanas = null to "error"
        val viewModel = nuevo(precios)

        viewModel.activar(true)

        val estado = viewModel.uiState.value
        assertEquals(4.0, estado.precio!!, delta)
        assertEquals("No se ha podido cargar la lista completa de semanas.", estado.aviso)
    }

    @Test
    fun `una respuesta de la cache se marca como desactualizada`() {
        val precios = FakePrecios()
        precios.ultimaSemana = referencia(ultima, desactualizado = true) to ""
        precios.semanas = referencia(ultima, desactualizado = true) to ""
        val viewModel = nuevo(precios)

        viewModel.activar(true)

        assertTrue(viewModel.uiState.value.desactualizado)
        assertEquals(5, viewModel.uiState.value.antiguedadHoras)
    }

    @Test
    fun `sin sesion no se piden precios y se avisa`() {
        val precios = FakePrecios()
        val viewModel = nuevo(precios, conSesion = false)

        viewModel.activar(true)

        assertEquals(0, precios.llamadasUltima)
        assertEquals(
            "Sesión caducada o no válida. Vuelve a iniciar sesión.",
            viewModel.uiState.value.aviso
        )
    }

    @Test
    fun `la lista ensena ocho semanas y se amplia con ver mas`() {
        val viewModel = nuevo()
        assertEquals(SEMANAS_POR_PAGINA, viewModel.uiState.value.semanasVisibles)

        viewModel.verMasSemanas()

        assertEquals(2 * SEMANAS_POR_PAGINA, viewModel.uiState.value.semanasVisibles)
    }
}
