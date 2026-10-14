package com.olivapp.app.voz.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import com.olivapp.app.campana.presentation.FakeCampanasRepositorio
import com.olivapp.app.cuaderno.presentation.FakeLaboresRepositorio
import com.olivapp.app.parcelas.presentation.FakeParcelasRepositorio
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Con Robolectric como el resto de la batería: la ficha del borrador es JSON
// (org.json), que en JVM sin Robolectric no está implementado.
@RunWith(RobolectricTestRunner::class)
/**
 * Tests del ViewModel del borrador IA de una nota (features
 * borrador-ia-desde-transcripcion, completar-borrador-por-preguntas,
 * confirmacion-de-borrador, borrador-sin-parcela y descarte-de-borrador).
 */
class BorradorViewModelTest {

    private val fichaConPregunta =
        """{"pregunta":"¿Cuántos kilos has recogido?","campos":{},"fecha":"2026-10-10","parcela":"Cañada Honda","categoria":"cosecha","observaciones":""}"""

    private val fichaCompleta =
        """{"pregunta":"","campos":{"cantidad":2500,"unidad":"kg"},"fecha":"2026-10-10","parcela":"Cañada Honda","categoria":"cosecha","observaciones":""}"""

    private fun sesionAbierta() = FakeSesionRepositorio().apply { guardar("token-abc") }

    private fun crear(
        sesion: FakeSesionRepositorio = sesionAbierta(),
        campanas: FakeCampanasRepositorio = FakeCampanasRepositorio(),
        parcelas: FakeParcelasRepositorio = FakeParcelasRepositorio(),
        labores: FakeLaboresRepositorio = FakeLaboresRepositorio(),
        notas: FakeNotasRepositorio = FakeNotasRepositorio()
    ) = BorradorViewModel(
        RuntimeEnvironment.getApplication(),
        sesion,
        campanas,
        parcelas,
        labores,
        notas,
        FakeGrabadorDeNotas()
    )

    @Test
    fun `al preparar carga las campañas y las parcelas si falta la parcela`() {
        val campanas = FakeCampanasRepositorio()
        val parcelas = FakeParcelasRepositorio()
        val viewModel = crear(campanas = campanas, parcelas = parcelas)

        viewModel.preparar("nota-1", """{"pregunta":"¿Cuántos kilos?","campos":{}}""")

        val estado = viewModel.uiState.value
        assertEquals(listOf("2024/2025"), estado.campanas.map { it.nombre })
        // Sin parcela en la ficha, hay que enseñar la lista del socio.
        assertEquals(listOf("Cañada Honda"), estado.parcelas)
        assertEquals("token-abc", parcelas.tokenLlamada)
    }

    @Test
    fun `con la parcela resuelta no se cargan las parcelas del socio`() {
        val parcelas = FakeParcelasRepositorio()
        val viewModel = crear(parcelas = parcelas)

        viewModel.preparar("nota-1", fichaCompleta)

        assertNull(parcelas.tokenLlamada)
    }

    @Test
    fun `elegir la parcela guarda la ficha y avisa a la cola`() {
        val viewModel = crear()
        viewModel.preparar("nota-1", """{"campos":{}}""")
        var guardada: String? = null

        viewModel.elegirParcela("Cañada Honda") { guardada = it }

        assertEquals("Cañada Honda", JSONObject(viewModel.uiState.value.fichaJson).optString("parcela"))
        assertEquals("Cañada Honda", JSONObject(guardada!!).optString("parcela"))
    }

    @Test
    fun `elegir la fecha guarda la ficha y cierra el calendario`() {
        val viewModel = crear()
        viewModel.preparar("nota-1", """{"campos":{}}""")
        var guardada: String? = null
        viewModel.abrirCalendario()

        viewModel.elegirFecha("2026-10-14") { guardada = it }

        val estado = viewModel.uiState.value
        assertFalse(estado.eligiendoFecha)
        assertEquals("2026-10-14", JSONObject(estado.fichaJson).optString("fecha"))
        assertEquals("2026-10-14", JSONObject(guardada!!).optString("fecha"))
    }

    @Test
    fun `confirmar con una parcela que no está en la lista no crea la labor`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = crear(labores = labores)
        viewModel.preparar("nota-1", fichaCompleta.replace("Cañada Honda", "La Perdiz"))
        var confirmado = false

        viewModel.confirmar { confirmado = true }

        assertFalse(confirmado)
        assertEquals(
            "No se ha encontrado la parcela del borrador en tu lista de parcelas. " +
                "Elígela antes de confirmar.",
            viewModel.uiState.value.avisoConfirmar
        )
        assertNull(labores.cuerpoLlamado)
    }

    @Test
    fun `confirmar crea la labor con el id de la nota y deja confirmado el borrador`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = crear(labores = labores)
        // La ficha guarda el nombre de la parcela; se resuelve por nombre,
        // sin distinguir mayúsculas ni la Ñ.
        viewModel.preparar("nota-1", fichaCompleta.replace("Cañada Honda", "cañada honda"))
        var confirmado = false

        viewModel.confirmar { confirmado = true }

        assertTrue(confirmado)
        assertNull(viewModel.uiState.value.avisoConfirmar)
        val cuerpo = labores.cuerpoLlamado!!
        assertEquals("2026-10-10", cuerpo.optString("fecha"))
        assertEquals("p-1", cuerpo.optString("parcela_id"))
        assertEquals("cosecha", cuerpo.optString("categoria"))
        // Una Nota de voz produce como máximo una Labor: el id de la nota hace
        // la creación idempotente en el backend.
        assertEquals("nota-1", cuerpo.optString("nota_id"))
    }

    @Test
    fun `sin sesión no se confirma y se avisa`() {
        val labores = FakeLaboresRepositorio()
        val viewModel = crear(sesion = FakeSesionRepositorio(), labores = labores)
        viewModel.preparar("nota-1", fichaCompleta)
        var confirmado = false

        viewModel.confirmar { confirmado = true }

        assertFalse(confirmado)
        assertEquals(
            "Sesión caducada o no válida. Vuelve a iniciar sesión.",
            viewModel.uiState.value.avisoConfirmar
        )
        assertNull(labores.cuerpoLlamado)
    }

    @Test
    fun `responder por voz completa la ficha y trae la siguiente pregunta`() {
        val notas = FakeNotasRepositorio()
        notas.resultadoCompletar = ResultadoDeRed(
            texto = "2.500 kilos de aceituna",
            borrador = """{"campos":{"cantidad":2500,"unidad":"kg"}}"""
        )
        val viewModel = crear(notas = notas)
        viewModel.preparar("nota-1", fichaConPregunta)
        var guardada: String? = null

        viewModel.empezarRespuesta { guardada = it }
        viewModel.pararRespuesta { guardada = it }

        val estado = viewModel.uiState.value
        assertEquals("2.500 kilos de aceituna", estado.ultimaRespuesta)
        assertEquals(2500, JSONObject(estado.fichaJson).optJSONObject("campos")!!.optInt("cantidad"))
        assertEquals(estado.fichaJson, guardada)
        assertFalse(estado.hayRespuestaSinEnviar)
        // La ficha se envía con la pregunta que se está respondiendo.
        assertEquals("token-abc", notas.completarLlamado?.first)
    }

    @Test
    fun `una respuesta sin conexión se conserva para reintentar`() {
        val notas = FakeNotasRepositorio()
        notas.resultadoCompletar = ResultadoDeRed(
            error = "Sin conexión con el servidor.",
            sinConexion = true
        )
        val viewModel = crear(notas = notas)
        viewModel.preparar("nota-1", fichaConPregunta)

        viewModel.empezarRespuesta { }
        viewModel.pararRespuesta { }

        val estado = viewModel.uiState.value
        // El audio no se tira: se reintenta sin tener que dictarlo otra vez.
        assertTrue(estado.hayRespuestaSinEnviar)
        assertEquals("Sin conexión con el servidor.", estado.aviso)
    }

    @Test
    fun `un audio inválido se tira y se pide volver a responder`() {
        val notas = FakeNotasRepositorio()
        notas.resultadoCompletar = ResultadoDeRed(
            error = "El audio no se puede usar.",
            audioInvalido = true
        )
        val viewModel = crear(notas = notas)
        viewModel.preparar("nota-1", fichaConPregunta)

        viewModel.empezarRespuesta { }
        viewModel.pararRespuesta { }

        val estado = viewModel.uiState.value
        assertFalse(estado.hayRespuestaSinEnviar)
        assertEquals("El audio no se puede usar.", estado.aviso)
    }

    @Test
    fun `al salir se borra una respuesta sin enviar`() {
        val viewModel = crear()
        viewModel.preparar("nota-1", fichaConPregunta)
        viewModel.empezarRespuesta { }

        viewModel.alSalir()

        // El audio de una respuesta sin enviar no se queda en el teléfono.
        assertFalse(viewModel.uiState.value.hayRespuestaSinEnviar)
    }

    @Test
    fun `al salir no se borra una respuesta que se está enviando`() {
        val notas = FakeNotasRepositorio(responderAlMomento = false)
        val viewModel = crear(notas = notas)
        viewModel.preparar("nota-1", fichaConPregunta)
        viewModel.empezarRespuesta { }
        viewModel.pararRespuesta { }

        viewModel.alSalir()

        assertTrue(viewModel.uiState.value.hayRespuestaSinEnviar)
    }

    @Test
    fun `descartar pide confirmación y avisa a la cola`() {
        val viewModel = crear()
        viewModel.preparar("nota-1", fichaConPregunta)
        var descartado = false

        viewModel.pedirDescartar()
        assertTrue(viewModel.uiState.value.preguntarDescarte)
        viewModel.cancelarDescartar()
        assertFalse(viewModel.uiState.value.preguntarDescarte)

        viewModel.pedirDescartar()
        viewModel.confirmarDescartar { descartado = true }

        assertTrue(descartado)
        assertFalse(viewModel.uiState.value.preguntarDescarte)
    }

    @Test
    fun `al preparar solo se siembra una vez`() {
        val viewModel = crear()
        viewModel.preparar("nota-1", fichaConPregunta)

        viewModel.preparar("nota-1", """{"campos":{}}""")

        // La rotación vuelve a llamar a preparar y la ficha no se machaca.
        assertEquals("2026-10-10", JSONObject(viewModel.uiState.value.fichaJson).optString("fecha"))
    }

    @Test
    fun `el aviso de micrófono se explica claro`() {
        val viewModel = crear()
        viewModel.preparar("nota-1", fichaConPregunta)

        viewModel.sinPermiso()

        assertEquals(
            "Sin permiso de micrófono no se puede responder por voz.",
            viewModel.uiState.value.aviso
        )
    }

    @Test
    fun `un borrador rechazado se puede descartar`() {
        val viewModel = crear()
        viewModel.preparar("nota-1", """{"rechazado":true}""")

        viewModel.pedirDescartar()

        assertTrue(viewModel.uiState.value.preguntarDescarte)
        assertNotNull(JSONObject(viewModel.uiState.value.fichaJson))
    }
}
