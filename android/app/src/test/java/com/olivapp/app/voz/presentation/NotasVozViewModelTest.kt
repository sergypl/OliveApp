package com.olivapp.app.voz.presentation

import com.olivapp.app.acceso.presentation.FakeSesionRepositorio
import com.olivapp.app.voz.data.SincronizadorNotas
import android.os.Looper
import com.olivapp.app.voz.domain.INTERVALO_AMPLITUD_MS
import com.olivapp.app.voz.domain.MUESTRAS_DE_ONDA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import java.io.File

// Con Robolectric como el resto de la batería: el sincronizador y la cola
// tocan clases de Android (Handler, org.json) que en JVM sin Robolectric no
// están implementadas.
@RunWith(RobolectricTestRunner::class)
/**
 * Tests del ViewModel de la pantalla de notas de voz (features notas-de-voz,
 * cola-offline-de-notas, reintento-de-transcripcion-fallida y
 * sincronizacion-pendientes-al-reconectar).
 */
class NotasVozViewModelTest {

    private fun sesionAbierta() = FakeSesionRepositorio().apply { guardar("token-abc") }

    private fun crear(
        notas: FakeNotasRepositorio = FakeNotasRepositorio(),
        grabador: FakeGrabadorDeNotas = FakeGrabadorDeNotas(),
        sesion: FakeSesionRepositorio = sesionAbierta()
    ) = NotasVozViewModel(RuntimeEnvironment.getApplication(), sesion, notas, grabador)

    @Test
    fun `la cola local se carga al abrir la pantalla`() {
        val notas = FakeNotasRepositorio()
        val nota = notas.guardar(File("nota-1.m4a"), 3)

        val viewModel = crear(notas)

        assertEquals(listOf(nota.id), viewModel.uiState.value.notas.map { it.id })
    }

    @Test
    fun `cuando el sincronizador avisa, la cola se vuelve a leer`() {
        val notas = FakeNotasRepositorio()
        val viewModel = crear(notas)
        assertTrue(viewModel.uiState.value.notas.isEmpty())

        // La cola cambia sola al recuperar la conexión y el SincronizadorNotas
        // lo avisa a la pantalla.
        notas.guardar(File("nota-2.m4a"), 5)
        SincronizadorNotas.alCambiar!!()

        assertEquals(listOf("nota-2.m4a"), viewModel.uiState.value.notas.map { it.id })
    }

    @Test
    fun `al salir de la pantalla se desengancha del sincronizador`() {
        val viewModel = crear()

        viewModel.alSalir()

        assertNull(SincronizadorNotas.alCambiar)
    }

    @Test
    fun `grabar y parar guarda la nota y la envía a transcribir`() {
        val notas = FakeNotasRepositorio(responderAlMomento = false)
        val grabador = FakeGrabadorDeNotas().apply { segundosGrabados = 3 }
        val viewModel = crear(notas, grabador)

        viewModel.empezarGrabacion()
        assertTrue(viewModel.uiState.value.grabando)

        viewModel.pararGrabacion()

        val estado = viewModel.uiState.value
        assertFalse(estado.grabando)
        // La nota queda guardada y en transcripción, con aviso visible.
        assertEquals("Nota de voz guardada. Transcribiendo…", estado.mensaje)
        assertEquals(listOf("nota-fake.m4a"), estado.notas.map { it.id })
        assertEquals(listOf("nota-fake.m4a", "transcribiendo", "", ""), notas.marcarLlamado)
        assertEquals("token-abc", notas.transcripcionLlamada?.first)
    }

    @Test
    fun `cuando la transcripción responde, la nota queda transcrita y se avisa`() {
        val notas = FakeNotasRepositorio(responderAlMomento = false)
        val viewModel = crear(notas)
        viewModel.empezarGrabacion()
        viewModel.pararGrabacion()

        // La respuesta del gateway llega después de enviar el audio.
        notas.pendienteTranscribir!!.invoke(
            "texto transcrito", """{"pregunta":"¿Cuántos kilos has recogido?"}""", "", false, false
        )

        val estado = viewModel.uiState.value
        assertEquals("Nota transcrita. Ya puedes leer el texto.", estado.mensaje)
        assertEquals("transcrita", estado.notas.first().estado)
    }

    @Test
    fun `una grabación demasiado corta se descarta con aviso`() {
        val notas = FakeNotasRepositorio()
        val grabador = FakeGrabadorDeNotas().apply { segundosGrabados = 0 }
        val viewModel = crear(notas, grabador)

        viewModel.empezarGrabacion()
        viewModel.pararGrabacion()

        val estado = viewModel.uiState.value
        assertEquals("La grabación ha sido demasiado corta. Vuelve a intentarlo.", estado.mensaje)
        // No se guarda ninguna nota y el audio se tira.
        assertTrue(estado.notas.isEmpty())
    }

    @Test
    fun `sin sesión la nota no se transcribe y queda con error`() {
        val notas = FakeNotasRepositorio()
        val nota = notas.guardar(File("nota-1.m4a"), 3)
        val viewModel = crear(notas, sesion = FakeSesionRepositorio())

        viewModel.transcribir(nota)

        val estado = viewModel.uiState.value
        assertEquals(listOf(nota.id, "error", "", ""), notas.marcarLlamado)
        assertEquals("Sesión caducada o no válida. Vuelve a iniciar sesión.", estado.mensaje)
    }

    @Test
    fun `una transcripción sin conexión devuelve la nota a la cola pendiente`() {
        val notas = FakeNotasRepositorio()
        notas.resultadoTranscribir = ResultadoDeRed(
            sinConexion = true,
            error = "Sin conexión con el servidor."
        )
        val nota = notas.guardar(File("nota-1.m4a"), 3)
        val viewModel = crear(notas)

        viewModel.transcribir(nota)

        val estado = viewModel.uiState.value
        // La nota no se pierde: vuelve a la cola local en estado 'pendiente'.
        assertEquals(listOf(nota.id, "pendiente", "", ""), notas.marcarLlamado)
        assertEquals(
            "Sin conexión con el servidor. Tu nota está guardada en el teléfono, pendiente de enviar.",
            estado.mensaje
        )
        assertEquals("pendiente", estado.notas.first().estado)
    }

    @Test
    fun `un audio inválido deja la nota lista para repetir el dictado`() {
        val notas = FakeNotasRepositorio()
        notas.resultadoTranscribir = ResultadoDeRed(
            error = "El audio no se puede transcribir.",
            audioInvalido = true
        )
        val nota = notas.guardar(File("nota-1.m4a"), 3)
        val viewModel = crear(notas)

        viewModel.transcribir(nota)

        val estado = viewModel.uiState.value
        assertEquals(listOf(nota.id, "audio_invalido", "", ""), notas.marcarLlamado)
        assertEquals("El audio no se puede transcribir.", estado.mensaje)
    }

    @Test
    fun `un fallo del servicio deja la nota con error visible y aviso`() {
        val notas = FakeNotasRepositorio()
        notas.resultadoTranscribir = ResultadoDeRed(error = "El servicio de transcripción falla.")
        val nota = notas.guardar(File("nota-1.m4a"), 3)
        val viewModel = crear(notas)

        viewModel.transcribir(nota)

        val estado = viewModel.uiState.value
        assertEquals(listOf(nota.id, "error", "", ""), notas.marcarLlamado)
        assertEquals("El servicio de transcripción falla.", estado.mensaje)
    }

    @Test
    fun `una transcripción con éxito deja la nota transcrita con su texto`() {
        val notas = FakeNotasRepositorio()
        val nota = notas.guardar(File("nota-1.m4a"), 3)
        val viewModel = crear(notas)

        viewModel.transcribir(nota)

        val estado = viewModel.uiState.value
        assertEquals(listOf(nota.id, "transcrita", "texto transcrito", """{"pregunta":"¿Cuántos kilos has recogido?"}"""), notas.marcarLlamado)
        assertEquals("Nota transcrita. Ya puedes leer el texto.", estado.mensaje)
    }

    @Test
    fun `guardar el borrador IA actualiza la ficha de la nota`() {
        val notas = FakeNotasRepositorio()
        val nota = notas.guardar(File("nota-1.m4a"), 3)
        val viewModel = crear(notas)

        viewModel.guardarBorrador(nota.id, """{"parcela":"Cañada Honda"}""")

        assertEquals(nota.id to """{"parcela":"Cañada Honda"}""", notas.actualizarBorradorLlamado)
    }

    @Test
    fun `confirmar el borrador deja la nota confirmada y descartar la descarta`() {
        val notas = FakeNotasRepositorio()
        val nota = notas.guardar(File("nota-1.m4a"), 3).let {
            notas.marcar(it.id, "transcrita", "2.500 kilos", """{"campos":{}}""")
            notas.notas().first()
        }
        val viewModel = crear(notas)

        viewModel.confirmarBorrador(nota)
        assertEquals(
            listOf(nota.id, "confirmada", nota.texto, nota.borrador),
            notas.marcarLlamado
        )

        viewModel.descartarBorrador(nota)
        assertEquals(
            listOf(nota.id, "descartada", nota.texto, nota.borrador),
            notas.marcarLlamado
        )
    }

    @Test
    fun `al salir se tira una grabación a medias y su audio no se queda`() {
        val notas = FakeNotasRepositorio()
        val grabador = FakeGrabadorDeNotas()
        val viewModel = crear(notas, grabador)
        viewModel.empezarGrabacion()

        viewModel.alSalir()

        assertFalse(viewModel.uiState.value.grabando)
        assertFalse(grabador.grabando)
    }

    @Test
    fun `mientras se graba la onda sigue la amplitud del microfono`() {
        val grabador = FakeGrabadorDeNotas().apply { amplitudActual = 20000 }
        val viewModel = crear(grabador = grabador)

        viewModel.empezarGrabacion()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(INTERVALO_AMPLITUD_MS * 3))

        val onda = viewModel.uiState.value.nivelesOnda
        assertEquals(MUESTRAS_DE_ONDA, onda.size)
        assertTrue(onda.takeLast(3).all { it > 0.5f })
        assertEquals(0f, onda.first(), 0f)
    }

    @Test
    fun `al parar la grabacion la onda vuelve al silencio`() {
        val notas = FakeNotasRepositorio(responderAlMomento = false)
        val grabador = FakeGrabadorDeNotas().apply { amplitudActual = 20000 }
        val viewModel = crear(notas, grabador)
        viewModel.empezarGrabacion()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(INTERVALO_AMPLITUD_MS * 2))

        viewModel.pararGrabacion()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(INTERVALO_AMPLITUD_MS * 2))

        assertTrue(viewModel.uiState.value.nivelesOnda.all { it == 0f })
    }

    @Test
    fun `una segunda pulsacion de transcribir no envia el audio otra vez`() {
        val notas = FakeNotasRepositorio(responderAlMomento = false)
        val nota = notas.guardar(File("nota-1.m4a"), 3)
        val viewModel = crear(notas)

        viewModel.transcribir(nota)
        viewModel.transcribir(nota)

        // La nota ya está en transcripción: la segunda pulsación se ignora.
        assertEquals(1, notas.transcribirLlamadas)
    }

    @Test
    fun `una nota que ya se esta transcribiendo no se vuelve a enviar`() {
        val notas = FakeNotasRepositorio(responderAlMomento = false)
        val nota = notas.guardar(File("nota-1.m4a"), 3)
        notas.marcar(nota.id, "transcribiendo", "")
        val viewModel = crear(notas)

        viewModel.transcribir(nota)

        assertEquals(0, notas.transcribirLlamadas)
    }

    @Test
    fun `doble pulsacion de grabar solo empieza una grabacion`() {
        val grabador = FakeGrabadorDeNotas()
        val viewModel = crear(grabador = grabador)

        viewModel.empezarGrabacion()
        viewModel.empezarGrabacion()

        assertEquals(1, grabador.empezarLlamadas)
        assertTrue(viewModel.uiState.value.grabando)
    }
}
