package com.olivapp.app.cuaderno.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import com.olivapp.app.cuaderno.domain.fechaDeHoy
import com.olivapp.app.parcelas.presentation.FakeParcelasRepositorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Con Robolectric como el resto de la batería: el formulario construye JSON
// (org.json), que en JVM sin Robolectric no está implementado.
@RunWith(RobolectricTestRunner::class)
/**
 * Tests del ViewModel del formulario de labor (features alta-manual-labor y
 * edicion-de-labor).
 */
class NuevaLaborViewModelTest {

    private fun sesionAbierta() = FakeSesionRepositorio().apply { guardar("token-abc") }

    @Test
    fun `el alta nueva arranca con la fecha de hoy y las parcelas del socio`() {
        val parcelas = FakeParcelasRepositorio()
        val viewModel = NuevaLaborViewModel(sesionAbierta(), FakeLaboresRepositorio(), parcelas)

        viewModel.preparar(null)

        val estado = viewModel.uiState.value
        assertFalse(estado.esEdicion)
        assertEquals(fechaDeHoy(), estado.fecha)
        assertEquals(listOf("Cañada Honda"), estado.parcelas.map { it.nombre })
    }

    @Test
    fun `editar una labor prellena el formulario con sus datos`() {
        val viewModel = NuevaLaborViewModel(
            sesionAbierta(),
            FakeLaboresRepositorio(),
            FakeParcelasRepositorio()
        )

        viewModel.preparar(laborDeEjemplo())

        val estado = viewModel.uiState.value
        assertTrue(estado.esEdicion)
        assertEquals("15/12/2025", estado.fecha)
        assertEquals("p-1", estado.parcelaId)
        assertEquals("Cañada Honda", estado.parcelaNombre)
        assertEquals("tratamiento", estado.categoria)
        assertEquals("Recinol", estado.producto)
        assertEquals("repilo", estado.plaga)
        assertEquals("2,5", estado.cantidad)
        assertEquals("L", estado.unidad)
        assertEquals("Con viento flojo", estado.observaciones)
    }

    @Test
    fun `al elegir tratamiento o riego la unidad anterior se vacia`() {
        val viewModel = NuevaLaborViewModel(
            sesionAbierta(),
            FakeLaboresRepositorio(),
            FakeParcelasRepositorio()
        )

        viewModel.preparar(laborDeEjemplo()) // venía con unidad "L"
        viewModel.elegirCategoria("riego")

        assertEquals("", viewModel.uiState.value.unidad)
        assertEquals("riego", viewModel.uiState.value.categoria)
    }

    @Test
    fun `una fecha que no se entiende no guarda y explica como escribirla`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = NuevaLaborViewModel(sesionAbierta(), labores, FakeParcelasRepositorio())

        viewModel.preparar(null)
        viewModel.cambiarFecha("32/13/2025")
        viewModel.guardar()

        val estado = viewModel.uiState.value
        assertEquals(
            "Escribe la fecha como día, mes y año (por ejemplo, 15/12/2025).",
            estado.mensaje
        )
        assertFalse(estado.trabajando)
        assertNull(labores.cuerpoLlamado)
    }

    @Test
    fun `guardar una labor nueva la envia al backend en su formato y ensena la campana`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = NuevaLaborViewModel(sesionAbierta(), labores, FakeParcelasRepositorio())

        viewModel.preparar(null)
        viewModel.cambiarFecha("15/12/2025")
        viewModel.elegirParcela("Cañada Honda")
        viewModel.elegirCategoria("tratamiento")
        viewModel.cambiarProducto("Recinol")
        viewModel.cambiarPlaga("repilo")
        viewModel.cambiarCantidad("2,5")
        viewModel.elegirUnidad("L")
        viewModel.guardar()

        val estado = viewModel.uiState.value
        val cuerpo = labores.cuerpoLlamado!!
        assertEquals("2025-12-15", cuerpo.getString("fecha"))
        assertEquals("p-1", cuerpo.getString("parcela_id"))
        assertEquals("tratamiento", cuerpo.getString("categoria"))
        assertEquals("Recinol", cuerpo.getJSONObject("campos").getString("producto"))
        // La cantidad viaja con punto decimal, como espera el backend.
        assertEquals("2.5", cuerpo.getJSONObject("campos").getString("cantidad"))
        assertTrue(estado.guardada)
        assertEquals("2025/2026", estado.campanaGuardada)
        assertEquals("Labor guardada en la campaña 2025/2026.", estado.mensaje)
    }

    @Test
    fun `editar una labor llama a actualizar con su id`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = NuevaLaborViewModel(sesionAbierta(), labores, FakeParcelasRepositorio())

        viewModel.preparar(laborDeEjemplo())
        viewModel.guardar()

        val estado = viewModel.uiState.value
        assertEquals("lab-7", labores.idLlamado)
        assertEquals("Cambios guardados en tu cuaderno.", estado.mensaje)
        assertTrue(estado.guardada)
        assertNull(estado.campanaGuardada)
    }

    @Test
    fun `guardar sin sesion no guarda nada y avisa`() {
        val labores = FakeLaboresRepositorio()
        val viewModel =
            NuevaLaborViewModel(FakeSesionRepositorio(), labores, FakeParcelasRepositorio())

        viewModel.preparar(null)
        viewModel.cambiarFecha("15/12/2025")
        viewModel.guardar()

        val estado = viewModel.uiState.value
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
        assertNull(labores.cuerpoLlamado)
    }

    @Test
    fun `preparar otra vez no machaca lo tecleado (el estado sobrevive a la rotacion)`() {
        val viewModel = NuevaLaborViewModel(
            sesionAbierta(),
            FakeLaboresRepositorio(),
            FakeParcelasRepositorio()
        )

        viewModel.preparar(null)
        viewModel.cambiarFecha("20/01/2026")
        viewModel.preparar(null) // lo que pasa al rotar: la llamada llega otra vez

        assertEquals("20/01/2026", viewModel.uiState.value.fecha)
    }
}
