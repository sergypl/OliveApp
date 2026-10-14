package com.olivapp.app.simulador.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.core.formatos.formatearCantidad
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.FondoCrema
import com.olivapp.app.core.ui.TextoError
import com.olivapp.app.core.ui.TextoMarron
import com.olivapp.app.core.ui.VerdeOliva
import com.olivapp.app.simulador.domain.ResultadoSimulacion
import com.olivapp.app.simulador.domain.numeroDeCampo
import com.olivapp.app.simulador.domain.recalcularConAjuste
import com.olivapp.app.simulador.domain.simularLiquidacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Estado visible del simulador: los tres campos y el resultado (o el error). */
data class SimuladorUiState(
    val kilos: String = "",
    val rendimiento: String = "",
    val ajuste: String = "10",
    val resultado: ResultadoSimulacion? = null,
    val error: String = ""
)

@HiltViewModel
class SimuladorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SimuladorUiState())
    val uiState: StateFlow<SimuladorUiState> = _uiState.asStateFlow()

    fun cambiarKilos(texto: String) {
        // Cambiar kilos o rendimiento borra el resultado: el cálculo va bajo
        // demanda con "Simular".
        _uiState.update { it.copy(kilos = texto, resultado = null) }
    }

    fun cambiarRendimiento(texto: String) {
        _uiState.update { it.copy(rendimiento = texto, resultado = null) }
    }

    fun cambiarAjuste(texto: String) {
        val actual = _uiState.value
        // Recálculo al instante: si ya hay resultado y el nuevo ajuste
        // es un porcentaje válido, el aceite neto se recalcula con él.
        val ajusteNuevo = numeroDeCampo(texto)
        val anterior = actual.resultado
        val resultado = if (anterior != null && ajusteNuevo != null &&
            ajusteNuevo >= 0.0 && ajusteNuevo <= 100.0
        ) {
            recalcularConAjuste(anterior, ajusteNuevo)
        } else {
            anterior
        }
        _uiState.update { it.copy(ajuste = texto, resultado = resultado) }
    }

    /** El cálculo va bajo demanda: solo con "Simular". */
    fun simular() {
        val actual = _uiState.value
        val kilosSimulados = numeroDeCampo(actual.kilos)
        val rendimientoSimulado = numeroDeCampo(actual.rendimiento)
        val ajusteSimulado = numeroDeCampo(actual.ajuste)
        when {
            kilosSimulados == null || kilosSimulados <= 0.0 -> {
                _uiState.update {
                    it.copy(
                        resultado = null,
                        error = "Introduce los kilos de aceituna, mayores que cero."
                    )
                }
            }
            rendimientoSimulado == null ||
                rendimientoSimulado <= 0.0 || rendimientoSimulado > 100.0 -> {
                _uiState.update {
                    it.copy(
                        resultado = null,
                        error = "Introduce el rendimiento graso como porcentaje " +
                            "entre 0 y 100 (por ejemplo, 20)."
                    )
                }
            }
            ajusteSimulado == null ||
                ajusteSimulado < 0.0 || ajusteSimulado > 100.0 -> {
                _uiState.update {
                    it.copy(
                        resultado = null,
                        error = "Introduce el ajuste industrial como porcentaje " +
                            "entre 0 y 100 (por ejemplo, 10)."
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        resultado = simularLiquidacion(
                            kilosSimulados, rendimientoSimulado, ajusteSimulado
                        ),
                        error = ""
                    )
                }
            }
        }
    }
}

/**
 * Simulador de liquidación en limpio (feature calculo-de-liquidacion-en-limpio):
 * el socio introduce kilos de aceituna y rendimiento graso y ve el aceite neto
 * estimado (kilos × rendimiento − ajuste industrial), claramente etiquetado
 * como estimación orientativa. La simulación es bajo demanda y no se guarda:
 * no compite con las cifras oficiales de la cooperativa (CONTEXT.md).
 *
 * El ajuste industrial es configurable (feature ajuste-industrial-configurable):
 * el campo arranca en el 10 % por defecto (ADR 0007) y, si ya hay un resultado,
 * cambiarlo recalcula el aceite neto al instante. Cambiar kilos o rendimiento
 * sigue borrando el resultado: el cálculo va bajo demanda con "Simular".
 */
@Composable
fun PantallaSimulador(
    onVolver: () -> Unit,
    viewModel: SimuladorViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Liquidación en limpio",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoMarron
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Simula tu liquidación en limpio con los kilos de aceituna " +
                "y el rendimiento graso.\nEs una estimación orientativa, no la " +
                "liquidación oficial de la cooperativa.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextoMarron,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CampoDeTexto(
            etiqueta = "Kilos de aceituna",
            valor = estado.kilos,
            alCambiar = viewModel::cambiarKilos,
            tipo = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Rendimiento graso (%)",
            valor = estado.rendimiento,
            alCambiar = viewModel::cambiarRendimiento,
            tipo = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Ajuste industrial (%)",
            valor = estado.ajuste,
            alCambiar = viewModel::cambiarAjuste,
            tipo = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(
            texto = "Simular",
            alPulsar = viewModel::simular
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (estado.error.isNotEmpty()) {
            Text(
                text = estado.error,
                fontSize = 20.sp,
                color = TextoError,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        estado.resultado?.let {
            Text(
                text = "Aceite neto estimado",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextoMarron
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatearCantidad(it.aceiteNeto) + " kg",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeOliva
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilaDato(
                etiqueta = "Aceite según el rendimiento",
                valor = formatearCantidad(it.aceiteSegunRendimiento) + " kg"
            )
            FilaDato(
                etiqueta = "Ajuste industrial (" +
                    formatearCantidad(it.ajustePorcentaje) + " %)",
                valor = "-" + formatearCantidad(it.ajusteIndustrial) + " kg"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Estimación orientativa: la liquidación oficial es la " +
                    "de la cooperativa.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TextoMarron,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        BotonSecundario(texto = "Volver al panel", alPulsar = onVolver)
    }
}
