package com.olivapp.app.simulador.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.core.formatos.formatearCantidad
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.Fondo
import com.olivapp.app.core.ui.PanelDestacado
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.Tarjeta
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoBoton
import com.olivapp.app.core.ui.TextoCifra
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.TextoSecundario
import com.olivapp.app.core.ui.TextoTitulo1
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.core.ui.pantallaCompleta
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
import com.olivapp.app.R

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
            .pantallaCompleta()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Liquidación en limpio",
            style = TextoTitulo1,
            color = Texto,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
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
        }
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(
            texto = "Simular",
            icono = R.drawable.ic_simulador,
            alPulsar = viewModel::simular
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (estado.error.isNotEmpty()) {
            Text(
                text = estado.error,
                style = TextoBoton,
                color = Error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        estado.resultado?.let {
            // Panel de resultado destacado (estilo del desglose del mockup).
            PanelDestacado {
                Text(
                    text = "Aceite neto estimado",
                    style = TextoValor,
                    color = Texto,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatearCantidad(it.aceiteNeto) + " kg",
                    style = TextoCifra,
                    color = Primario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                FilaDato(
                    etiqueta = "Aceite según el rendimiento",
                    valor = formatearCantidad(it.aceiteSegunRendimiento) + " kg",
                    icono = R.drawable.ic_aceite
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilaDato(
                    etiqueta = "Ajuste industrial (" +
                        formatearCantidad(it.ajustePorcentaje) + " %)",
                    valor = "-" + formatearCantidad(it.ajusteIndustrial) + " kg",
                    icono = R.drawable.ic_kilos
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Estimación orientativa: la liquidación oficial es la " +
                    "de la cooperativa.",
                style = TextoCuerpo,
                color = TextoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        BotonSecundario(texto = "Volver al panel", icono = R.drawable.ic_volver, alPulsar = onVolver)
    }
}
