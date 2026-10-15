package com.olivapp.app.simulador.presentation

import androidx.lifecycle.ViewModel
import com.olivapp.app.acceso.domain.SesionRepositorio
import com.olivapp.app.simulador.domain.CategoriaAceite
import com.olivapp.app.simulador.domain.MercadoAceite
import com.olivapp.app.simulador.domain.PreciosAceiteRepositorio
import com.olivapp.app.simulador.domain.PreferenciasSimulador
import com.olivapp.app.simulador.domain.RepartoCalidad
import com.olivapp.app.simulador.domain.SemanaDePrecios
import com.olivapp.app.simulador.domain.campanasDisponibles
import com.olivapp.app.simulador.domain.formatearNumeroDeCampo
import com.olivapp.app.simulador.domain.numeroDeCampo
import com.olivapp.app.simulador.domain.precioPonderado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject

/** De dónde sale el precio del aceite: consultado (pasado o última semana) o escrito a mano. */
enum class ModoPrecio { CONSULTADO, MANUAL }

/** Estado del simulador avanzado (precio del aceite, gastos y reparto por calidad). */
data class AvanzadoUiState(
    val activado: Boolean = false,
    val modo: ModoPrecio = ModoPrecio.CONSULTADO,
    val categoria: CategoriaAceite = CategoriaAceite.AOVE,
    val mercado: MercadoAceite = MercadoAceite.MEDIA_NACIONAL,
    val campana: String = "",
    val campanas: List<String> = emptyList(),
    val semanas: List<SemanaDePrecios> = emptyList(),
    /** Lunes (ISO) de la semana elegida de la lista, o null si no hay ninguna. */
    val semanaElegida: String? = null,
    /** Cuántas semanas enseña la lista; se amplía con "Ver más semanas". */
    val semanasVisibles: Int = SEMANAS_POR_PAGINA,
    val cargando: Boolean = false,
    /** Fallo de la carga de precios: la entrada manual sigue funcionando. */
    val aviso: String = "",
    val fuente: String = "",
    val desactualizado: Boolean = false,
    val antiguedadHoras: Int? = null,
    val precioManual: String = "",
    val gastos: String = "0,20",
    val aove: String = "100",
    val virgen: String = "0",
    val lampante: String = "0"
) {
    val semanaUsada: SemanaDePrecios?
        get() = semanas.firstOrNull { it.semana == semanaElegida }

    /** Reparto tecleado; un campo vacío o ilegible cuenta como 0 %. */
    val reparto: RepartoCalidad
        get() = RepartoCalidad(
            aove = numeroDeCampo(aove) ?: 0.0,
            virgen = numeroDeCampo(virgen) ?: 0.0,
            lampante = numeroDeCampo(lampante) ?: 0.0
        )

    /** Con precio consultado hay reparto por calidad; con precio manual no (precio único). */
    val repartoVisible: Boolean get() = modo == ModoPrecio.CONSULTADO && semanaUsada != null

    /** Precio usado en €/kg: el manual, o el ponderado de la semana elegida. Null si falta. */
    val precio: Double?
        get() = when (modo) {
            ModoPrecio.MANUAL -> numeroDeCampo(precioManual)
            ModoPrecio.CONSULTADO -> semanaUsada?.let { precioPonderado(it.precios, reparto) }
        }

    val gastosPorKilo: Double? get() = numeroDeCampo(gastos)

    /** Mensaje cuando el reparto tecleado no suma 100 %; vacío si todo va bien. */
    val avisoDeReparto: String
        get() = if (repartoVisible && !reparto.sumaCien) {
            "El reparto suma ${formatearPorcentaje(reparto.suma)} %. Tiene que sumar 100 %."
        } else {
            ""
        }
}

const val SEMANAS_POR_PAGINA = 8

private fun formatearPorcentaje(valor: Double): String =
    com.olivapp.app.core.formatos.formatearCantidad(valor)

@HiltViewModel
class SimuladorAvanzadoViewModel @Inject constructor(
    private val sesion: SesionRepositorio,
    private val precios: PreciosAceiteRepositorio,
    private val preferencias: PreferenciasSimulador
) : ViewModel() {

    private val campanas: List<String> = Calendar.getInstance().let {
        campanasDisponibles(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1)
    }

    private val _uiState = MutableStateFlow(
        AvanzadoUiState(
            activado = preferencias.avanzadoActivado(),
            campanas = campanas,
            campana = campanas.first()
        )
    )
    val uiState: StateFlow<AvanzadoUiState> = _uiState.asStateFlow()

    /** Contador para descartar respuestas de consultas ya sustituidas por otra. */
    private var consulta = 0

    init {
        if (_uiState.value.activado) cargarUltimaSemanaSiHaceFalta()
    }

    /** Interruptor "Simulador avanzado": se recuerda entre sesiones. */
    fun activar(activado: Boolean) {
        preferencias.guardarAvanzadoActivado(activado)
        _uiState.update { it.copy(activado = activado) }
        if (activado) cargarUltimaSemanaSiHaceFalta()
    }

    fun cambiarModo(modo: ModoPrecio) {
        _uiState.update { it.copy(modo = modo) }
        if (modo == ModoPrecio.CONSULTADO) cargarUltimaSemanaSiHaceFalta()
    }

    /** La categoría elegida se reparte al 100 % hasta que el socio cambie el reparto. */
    fun cambiarCategoria(categoria: CategoriaAceite) {
        val reparto = RepartoCalidad.completo(categoria)
        _uiState.update {
            it.copy(
                categoria = categoria,
                aove = formatearPorcentaje(reparto.aove),
                virgen = formatearPorcentaje(reparto.virgen),
                lampante = formatearPorcentaje(reparto.lampante)
            )
        }
    }

    fun cambiarMercado(mercado: MercadoAceite) {
        _uiState.update { it.copy(mercado = mercado, semanaElegida = null) }
        cargarSemanas()
    }

    fun cambiarCampana(campana: String) {
        _uiState.update { it.copy(campana = campana, semanaElegida = null) }
        cargarSemanas()
    }

    fun elegirSemana(semana: String) {
        _uiState.update { it.copy(semanaElegida = semana) }
    }

    fun verMasSemanas() {
        _uiState.update { it.copy(semanasVisibles = it.semanasVisibles + SEMANAS_POR_PAGINA) }
    }

    fun cambiarPrecioManual(texto: String) {
        _uiState.update { it.copy(precioManual = formatearNumeroDeCampo(texto)) }
    }

    fun cambiarGastos(texto: String) {
        _uiState.update { it.copy(gastos = formatearNumeroDeCampo(texto)) }
    }

    fun cambiarReparto(categoria: CategoriaAceite, texto: String) {
        val formateado = formatearNumeroDeCampo(texto)
        _uiState.update {
            when (categoria) {
                CategoriaAceite.AOVE -> it.copy(aove = formateado)
                CategoriaAceite.VIRGEN -> it.copy(virgen = formateado)
                CategoriaAceite.LAMPANTE -> it.copy(lampante = formateado)
            }
        }
    }

    /** Atajo "Última semana disponible": elige la semana más reciente del mercado. */
    fun elegirUltimaSemana() {
        val token = sesion.token()
        if (token == null) {
            _uiState.update { it.copy(aviso = SESION_CADUCADA) }
            return
        }
        val numero = ++consulta
        val mercado = _uiState.value.mercado
        _uiState.update { it.copy(cargando = true, aviso = "") }
        precios.cargarUltimaSemana(token, mercado) { referencia, mensaje ->
            if (numero != consulta) return@cargarUltimaSemana
            val semana = referencia?.semanas?.firstOrNull()
            if (referencia == null || semana == null) {
                _uiState.update { it.copy(cargando = false, aviso = mensaje.ifEmpty { SIN_PRECIOS }) }
                return@cargarUltimaSemana
            }
            _uiState.update {
                it.copy(
                    cargando = false,
                    campana = semana.campana.ifEmpty { it.campana },
                    semanas = referencia.semanas,
                    semanaElegida = semana.semana,
                    semanasVisibles = SEMANAS_POR_PAGINA,
                    fuente = referencia.fuente,
                    desactualizado = referencia.desactualizado,
                    antiguedadHoras = referencia.antiguedadHoras
                )
            }
            // La lista completa de la campaña, conservando la semana elegida.
            cargarSemanas()
        }
    }

    private fun cargarUltimaSemanaSiHaceFalta() {
        val estado = _uiState.value
        if (estado.modo == ModoPrecio.CONSULTADO && estado.semanas.isEmpty() && !estado.cargando) {
            elegirUltimaSemana()
        }
    }

    private fun cargarSemanas() {
        val token = sesion.token()
        if (token == null) {
            _uiState.update { it.copy(aviso = SESION_CADUCADA) }
            return
        }
        val numero = ++consulta
        val estado = _uiState.value
        _uiState.update { it.copy(cargando = true, aviso = "", semanasVisibles = SEMANAS_POR_PAGINA) }
        precios.cargarSemanas(token, estado.mercado, estado.campana) { referencia, mensaje ->
            if (numero != consulta) return@cargarSemanas
            if (referencia == null) {
                // Se conserva lo que hubiera: sin lista nueva, la elegida sigue valiendo.
                _uiState.update {
                    it.copy(
                        cargando = false,
                        aviso = if (it.semanas.isEmpty()) {
                            mensaje.ifEmpty { SIN_PRECIOS }
                        } else {
                            SIN_LISTA
                        }
                    )
                }
                return@cargarSemanas
            }
            _uiState.update {
                // Se conserva la semana elegida; si no, la más reciente de la lista.
                val conservada = it.semanaElegida?.takeIf { elegida ->
                    referencia.semanas.any { semana -> semana.semana == elegida }
                } ?: referencia.semanas.firstOrNull()?.semana
                it.copy(
                    cargando = false,
                    semanas = referencia.semanas,
                    semanaElegida = conservada,
                    fuente = referencia.fuente,
                    desactualizado = referencia.desactualizado,
                    antiguedadHoras = referencia.antiguedadHoras,
                    aviso = if (referencia.semanas.isEmpty()) {
                        "No hay precios de esa campaña en ese mercado. " +
                            "Prueba con otro o escribe el precio a mano."
                    } else {
                        ""
                    }
                )
            }
        }
    }

    private companion object {
        const val SESION_CADUCADA = "Sesión caducada o no válida. Vuelve a iniciar sesión."
        const val SIN_LISTA = "No se ha podido cargar la lista completa de semanas."
        const val SIN_PRECIOS =
            "No se han podido cargar los precios. Puedes escribir el precio a mano."
    }
}
