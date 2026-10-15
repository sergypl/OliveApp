package com.olivapp.app.simulador.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.core.formatos.formatearCantidad
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.ContenedorMasAlto
import com.olivapp.app.core.ui.CifraAnimada
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.FormasOlivApp
import com.olivapp.app.core.ui.PantallaConBarraSuperiorGrande
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.Tarjeta
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoBoton
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoSecundario
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.simulador.domain.ResultadoSimulacion
import com.olivapp.app.simulador.domain.formatearNumeroDeCampo
import com.olivapp.app.simulador.domain.numeroDeCampo
import com.olivapp.app.simulador.domain.recalcularConAjuste
import com.olivapp.app.simulador.domain.simularLiquidacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.olivapp.app.R

/** Estado visible del simulador: los tres campos y el resultado (o el aviso). */
data class SimuladorUiState(
    val kilos: String = "",
    val rendimiento: String = "",
    val ajuste: String = "10",
    val resultado: ResultadoSimulacion? = null,
    /** Valor fuera de rango: se muestra como error. */
    val error: String = "",
    /** Estado neutro (campo vacío o ilegible): no es un error. */
    val neutro: String = MENSAJE_NEUTRO
)

/** Estado neutro del simulador mientras faltan datos (no es un error). */
const val MENSAJE_NEUTRO = "Escribe los kilos y el rendimiento"

@HiltViewModel
class SimuladorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SimuladorUiState())
    val uiState: StateFlow<SimuladorUiState> = _uiState.asStateFlow()

    /** El resultado se recalcula al teclear: no hay botón "Simular". */
    fun cambiarKilos(texto: String) {
        _uiState.update { calcular(it.copy(kilos = formatearNumeroDeCampo(texto))) }
    }

    /** El resultado se recalcula al teclear: no hay botón "Simular". */
    fun cambiarRendimiento(texto: String) {
        _uiState.update { calcular(it.copy(rendimiento = formatearNumeroDeCampo(texto))) }
    }

    /** Rendimiento del deslizador (10 a 30 %): un decimal, sincronizado. */
    fun cambiarRendimientoConDeslizador(valor: Double) {
        val redondeado = Math.round(valor * 10.0) / 10.0
        _uiState.update {
            calcular(it.copy(rendimiento = formatearNumeroDeCampo(formatearCantidad(redondeado))))
        }
    }

    /** Botón "+": sube el rendimiento en 0,1 % (entre 10 y 30 %). */
    fun subirRendimiento() {
        cambiarRendimientoConDeslizador(pasoDeRendimiento(+1))
    }

    /** Botón "−": baja el rendimiento en 0,1 % (entre 10 y 30 %). */
    fun bajarRendimiento() {
        cambiarRendimientoConDeslizador(pasoDeRendimiento(-1))
    }

    private fun pasoDeRendimiento(salto: Int): Double {
        val tecleado = numeroDeCampo(_uiState.value.rendimiento) ?: MINIMO_RENDIMIENTO
        val actual = tecleado.coerceIn(MINIMO_RENDIMIENTO, MAXIMO_RENDIMIENTO)
        val nuevo = Math.round((actual + salto * 0.1) * 10.0) / 10.0
        return nuevo.coerceIn(MINIMO_RENDIMIENTO, MAXIMO_RENDIMIENTO)
    }

    fun cambiarAjuste(texto: String) {
        _uiState.update { estado ->
            val ajusteTecleado = numeroDeCampo(formatearNumeroDeCampo(texto))
            val anterior = estado.resultado
            if (anterior != null && ajusteTecleado != null &&
                ajusteTecleado >= 0.0 && ajusteTecleado <= 100.0
            ) {
                // Solo cambia la deducción: el aceite según el rendimiento no se toca.
                estado.copy(
                    ajuste = formatearNumeroDeCampo(texto),
                    resultado = recalcularConAjuste(anterior, ajusteTecleado),
                    error = "",
                    neutro = ""
                )
            } else {
                calcular(estado.copy(ajuste = formatearNumeroDeCampo(texto)))
            }
        }
    }

    /**
     * Cálculo en vivo de la simulación. Con campos vacíos o ilegibles no hay
     * error: se muestra el estado neutro. El ajuste vacío o ilegible usa el
     * 10 % por defecto (ADR 0007).
     */
    private fun calcular(estado: SimuladorUiState): SimuladorUiState {
        val kilos = numeroDeCampo(estado.kilos)
        val rendimiento = numeroDeCampo(estado.rendimiento)
        val ajusteTecleado = numeroDeCampo(estado.ajuste)
        if (kilos == null || rendimiento == null) {
            return estado.copy(resultado = null, error = "", neutro = MENSAJE_NEUTRO)
        }
        if (kilos <= 0.0) {
            return estado.copy(
                resultado = null,
                neutro = "",
                error = "Introduce los kilos de aceituna, mayores que cero."
            )
        }
        if (rendimiento <= 0.0 || rendimiento > 100.0) {
            return estado.copy(
                resultado = null,
                neutro = "",
                error = "Introduce el rendimiento graso como porcentaje " +
                    "entre 0 y 100 (por ejemplo, 20)."
            )
        }
        if (ajusteTecleado != null && (ajusteTecleado < 0.0 || ajusteTecleado > 100.0)) {
            return estado.copy(
                resultado = null,
                neutro = "",
                error = "Introduce el ajuste industrial como porcentaje " +
                    "entre 0 y 100 (por ejemplo, 10)."
            )
        }
        return estado.copy(
            resultado = simularLiquidacion(
                kilos, rendimiento, ajusteTecleado ?: 10.0
            ),
            error = "",
            neutro = ""
        )
    }

    private companion object {
        const val MINIMO_RENDIMIENTO = 10.0
        const val MAXIMO_RENDIMIENTO = 30.0
    }
}

/**
 * Simulador de liquidación en limpio (features calculo-de-liquidacion-en-limpio,
 * ajuste-industrial-configurable y simulador-calculo-en-vivo): el socio introduce
 * kilos de aceituna y rendimiento graso y ve el aceite neto estimado
 * (kilos × rendimiento − ajuste industrial), claramente etiquetado como
 * estimación orientativa. El resultado se recalcula al teclear (sin botón
 * "Simular") y no se guarda: no compite con las cifras oficiales de la
 * cooperativa (CONTEXT.md).
 *
 * La tarjeta de resultado está siempre visible (fijada sobre el formulario),
 * con la cifra animada al cambiar. Los campos llevan su unidad dentro, el
 * teclado es numérico y los millares se formatean al escribir. El rendimiento
 * graso se mueve también con botones de 0,1 % y con un deslizador de 10 a 30 %,
 * los tres sincronizados. Debajo, un interruptor "Simulador avanzado" despliega el
 * precio del aceite, los gastos de la cooperativa y el reparto por calidad
 * (feature simulador-avanzado-precio-aceite).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSimulador(
    viewModel: SimuladorViewModel = hiltViewModel(),
    avanzado: SimuladorAvanzadoViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val estadoAvanzado by avanzado.uiState.collectAsStateWithLifecycle()

    PantallaConBarraSuperiorGrande(
        titulo = "Liquidación en limpio",
        onVolver = null,
        fijo = { TarjetaDeResultado(estado) }
    ) {
        DetalleDeResultado(estado)
        Text(
            text = "Simula tu liquidación en limpio con los kilos de aceituna " +
                "y el rendimiento graso.\nEs una estimación orientativa, no la " +
                "liquidación oficial de la cooperativa.",
            style = TextoCuerpo,
            color = TextoSecundario,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Entradas agrupadas en tarjeta (sección de formulario del mockup).
        Tarjeta {
            CampoDeTexto(
                etiqueta = "Kilos de aceituna",
                valor = estado.kilos,
                alCambiar = viewModel::cambiarKilos,
                tipo = KeyboardType.Decimal,
                sufijo = "kg",
                formato = ::formatearNumeroDeCampo
            )
            Spacer(modifier = Modifier.height(16.dp))
            CampoDeTexto(
                etiqueta = "Rendimiento graso",
                valor = estado.rendimiento,
                alCambiar = viewModel::cambiarRendimiento,
                tipo = KeyboardType.Decimal,
                sufijo = "%",
                formato = ::formatearNumeroDeCampo
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BotonDePaso(
                    texto = "−0,1",
                    descripcion = "Bajar el rendimiento graso 0,1 por ciento",
                    alPulsar = viewModel::bajarRendimiento
                )
                Slider(
                    value = (numeroDeCampo(estado.rendimiento)?.toFloat() ?: 10f)
                        .coerceIn(10f, 30f),
                    onValueChange = { viewModel.cambiarRendimientoConDeslizador(it.toDouble()) },
                    valueRange = 10f..30f,
                    // Continuo: el ViewModel redondea a 0,1. Con 199 pasos se dibujaban
                    // 200 marcas que, con la paleta del tema, formaban una barra naranja
                    // (el naranja es solo del micrófono).
                    colors = SliderDefaults.colors(
                        thumbColor = Primario,
                        activeTrackColor = Primario,
                        inactiveTrackColor = ContenedorMasAlto
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                BotonDePaso(
                    texto = "+0,1",
                    descripcion = "Subir el rendimiento graso 0,1 por ciento",
                    alPulsar = viewModel::subirRendimiento
                )
            }
            Text(
                text = "Botones de 0,1 % y deslizador de 10 a 30 %.",
                style = TextoCuerpo,
                color = TextoSecundario,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            CampoDeTexto(
                etiqueta = "Ajuste industrial",
                valor = estado.ajuste,
                alCambiar = viewModel::cambiarAjuste,
                tipo = KeyboardType.Decimal,
                sufijo = "%",
                formato = ::formatearNumeroDeCampo
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        // Simulador avanzado (simulador-avanzado-precio-aceite): el básico no cambia.
        InterruptorAvanzado(activado = estadoAvanzado.activado, alCambiar = avanzado::activar)
        if (estadoAvanzado.activado) {
            Spacer(modifier = Modifier.height(24.dp))
            SeccionAvanzada(
                estado = estadoAvanzado,
                viewModel = avanzado,
                aceiteNeto = estado.resultado?.aceiteNeto,
                kilos = numeroDeCampo(estado.kilos)
            )
        }
    }
}

/**
 * Resultado compacto, fijado sobre el formulario: solo la cifra (o el aviso).
 * El desglose y el aviso largo van en [DetalleDeResultado], dentro del scroll:
 * una tarjeta fija alta se comía dos tercios de la pantalla y dejaba un hueco
 * mínimo para el formulario.
 */
@Composable
private fun TarjetaDeResultado(estado: SimuladorUiState) {
    Tarjeta {
        Text(
            text = "Aceite neto estimado",
            style = TextoValor,
            color = Texto,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        val resultado = estado.resultado
        if (resultado != null) {
            CifraAnimada(
                valor = resultado.aceiteNeto,
                sufijo = " kg",
                modificador = Modifier.fillMaxWidth()
            )
            Text(
                text = "Estimación orientativa",
                style = TextoCuerpo,
                color = TextoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Estado neutro o aviso: la tarjeta sigue visible, sin cifra.
            Text(
                text = estado.error.ifEmpty { estado.neutro },
                style = TextoCuerpo,
                color = if (estado.error.isEmpty()) TextoSecundario else Error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Desglose del resultado (aceite según el rendimiento y ajuste industrial), en el scroll. */
@Composable
private fun DetalleDeResultado(estado: SimuladorUiState) {
    val resultado = estado.resultado ?: return
    Tarjeta {
        FilaDato(
            etiqueta = "Aceite según el rendimiento",
            valor = formatearCantidad(resultado.aceiteSegunRendimiento) + " kg",
            icono = R.drawable.ic_aceite
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilaDato(
            etiqueta = "Ajuste industrial (" +
                formatearCantidad(resultado.ajustePorcentaje) + " %)",
            valor = "-" + formatearCantidad(resultado.ajusteIndustrial) + " kg",
            icono = R.drawable.ic_kilos
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

/** Botón grande de paso de 0,1 % (zona táctil de 64dp y texto, no solo símbolo). */
@Composable
private fun BotonDePaso(texto: String, descripcion: String, alPulsar: () -> Unit) {
    OutlinedButton(
        onClick = alPulsar,
        modifier = Modifier
            .height(64.dp)
            .widthIn(min = 64.dp)
            .semantics(mergeDescendants = true) { contentDescription = descripcion },
        shape = FormasOlivApp.medium,
        border = BorderStroke(2.dp, Primario),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(text = texto, style = TextoBoton, color = Primario)
    }
}
