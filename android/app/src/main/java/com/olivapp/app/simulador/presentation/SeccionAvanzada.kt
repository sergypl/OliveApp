package com.olivapp.app.simulador.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olivapp.app.core.formatos.formatearFecha
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.Borde
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.ChipEstado
import com.olivapp.app.core.ui.ChipVerdeFondo
import com.olivapp.app.core.ui.ChipVerdeTexto
import com.olivapp.app.core.ui.ContenedorBajo
import com.olivapp.app.core.ui.Desplegable
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.FormasOlivApp
import com.olivapp.app.core.ui.OpcionCategoria
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.Superficie
import com.olivapp.app.core.ui.Tarjeta
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoSecundario
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.simulador.domain.CategoriaAceite
import com.olivapp.app.simulador.domain.MercadoAceite
import com.olivapp.app.simulador.domain.SemanaDePrecios
import com.olivapp.app.simulador.domain.calcularImporteAvanzado
import com.olivapp.app.simulador.domain.formatearEuros
import com.olivapp.app.simulador.domain.formatearEurosPorKilo
import com.olivapp.app.simulador.domain.precioPonderado

const val AVISO_PRECIO_DE_REFERENCIA =
    "Precio de referencia en origen a granel. No es lo que paga tu cooperativa."

/** Interruptor "Simulador avanzado": fila entera pulsable, de 64dp como mínimo. */
@Composable
fun InterruptorAvanzado(activado: Boolean, alCambiar: (Boolean) -> Unit) {
    Tarjeta {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(value = activado, role = Role.Switch, onValueChange = alCambiar),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Simulador avanzado", style = TextoValor, color = Texto)
                Text(
                    text = "Añade el precio del aceite y los gastos de la cooperativa.",
                    style = TextoCuerpo,
                    color = TextoSecundario
                )
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            // La fila entera ya es el control: el interruptor solo lo representa.
            Switch(checked = activado, onCheckedChange = null)
        }
    }
}

/**
 * Parte avanzada del simulador: precio del aceite (consultado o a mano), gastos
 * de la cooperativa y reparto por calidad, y el importe estimado que sale con el
 * aceite neto del simulador básico. Todo es una estimación, nunca la liquidación
 * en limpio de la cooperativa.
 */
@Composable
fun SeccionAvanzada(
    estado: AvanzadoUiState,
    viewModel: SimuladorAvanzadoViewModel,
    aceiteNeto: Double?,
    kilos: Double?
) {
    Tarjeta {
        Text(text = "Precio del aceite (€/kg)", style = TextoValor, color = Texto)
        Spacer(modifier = Modifier.height(16.dp))
        OpcionCategoria(
            texto = "Consultar un precio de referencia",
            elegida = estado.modo == ModoPrecio.CONSULTADO,
            alPulsar = { viewModel.cambiarModo(ModoPrecio.CONSULTADO) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OpcionCategoria(
            texto = "Escribirlo a mano",
            elegida = estado.modo == ModoPrecio.MANUAL,
            alPulsar = { viewModel.cambiarModo(ModoPrecio.MANUAL) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (estado.modo == ModoPrecio.MANUAL) {
            CampoDeTexto(
                etiqueta = "Precio del aceite",
                valor = estado.precioManual,
                alCambiar = viewModel::cambiarPrecioManual,
                tipo = KeyboardType.Decimal,
                sufijo = "€/kg"
            )
        } else {
            SeleccionDePrecio(estado, viewModel)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    Tarjeta {
        Text(text = "Gastos de la cooperativa", style = TextoValor, color = Texto)
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Gastos por kilo de aceite",
            valor = estado.gastos,
            alCambiar = viewModel::cambiarGastos,
            tipo = KeyboardType.Decimal,
            sufijo = "€/kg"
        )
        if (estado.repartoVisible) {
            Spacer(modifier = Modifier.height(24.dp))
            RepartoPorCalidad(estado, viewModel)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    ResultadoAvanzadoTarjeta(estado, aceiteNeto, kilos)
}

@Composable
private fun SeleccionDePrecio(estado: AvanzadoUiState, viewModel: SimuladorAvanzadoViewModel) {
    Desplegable(
        etiqueta = "Categoría",
        valor = estado.categoria.etiqueta,
        opciones = CategoriaAceite.entries.map { it.etiqueta },
        alElegir = { etiqueta ->
            CategoriaAceite.entries.firstOrNull { it.etiqueta == etiqueta }
                ?.let(viewModel::cambiarCategoria)
        }
    )
    Spacer(modifier = Modifier.height(16.dp))
    Desplegable(
        etiqueta = "Mercado",
        valor = estado.mercado.etiqueta,
        opciones = MercadoAceite.entries.map { it.etiqueta },
        alElegir = { etiqueta ->
            MercadoAceite.entries.firstOrNull { it.etiqueta == etiqueta }
                ?.let(viewModel::cambiarMercado)
        }
    )
    Spacer(modifier = Modifier.height(16.dp))
    Desplegable(
        etiqueta = "Campaña",
        valor = estado.campana,
        opciones = estado.campanas,
        alElegir = viewModel::cambiarCampana
    )
    Spacer(modifier = Modifier.height(16.dp))
    BotonSecundario(texto = "Última semana disponible", alPulsar = viewModel::elegirUltimaSemana)
    Spacer(modifier = Modifier.height(16.dp))
    if (estado.cargando) {
        Text(text = "Cargando precios…", style = TextoCuerpo, color = TextoSecundario)
    }
    if (estado.aviso.isNotEmpty()) {
        Text(text = estado.aviso, style = TextoCuerpo, color = Error)
    }
    if (estado.desactualizado) {
        Text(
            text = "No se ha podido consultar la fuente: estos precios están guardados" +
                (estado.antiguedadHoras?.let { " (de hace ${textoAntiguedad(it)})" } ?: "") + ".",
            style = TextoCuerpo,
            color = TextoSecundario
        )
    }
    if (estado.semanas.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Elige la semana (precio de ${estado.categoria.etiqueta})",
            style = TextoCuerpo,
            color = TextoSecundario
        )
        Spacer(modifier = Modifier.height(8.dp))
        for (semana in estado.semanas.take(estado.semanasVisibles)) {
            FilaDeSemana(
                semana = semana,
                categoria = estado.categoria,
                elegida = semana.semana == estado.semanaElegida,
                alPulsar = { viewModel.elegirSemana(semana.semana) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (estado.semanas.size > estado.semanasVisibles) {
            BotonSecundario(texto = "Ver más semanas", alPulsar = viewModel::verMasSemanas)
        }
    }
}

/** Semana de la lista con su precio al lado; la elegida lleva borde y la etiqueta "Elegida". */
@Composable
private fun FilaDeSemana(
    semana: SemanaDePrecios,
    categoria: CategoriaAceite,
    elegida: Boolean,
    alPulsar: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectable(selected = elegida, role = Role.RadioButton, onClick = alPulsar),
        shape = FormasOlivApp.medium,
        color = if (elegida) ContenedorBajo else Superficie,
        border = BorderStroke(if (elegida) 2.dp else 1.dp, if (elegida) Primario else Borde)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Semana del ${formatearFecha(semana.semana)}",
                    style = TextoCuerpo,
                    color = Texto
                )
                Text(
                    text = "al ${formatearFecha(semana.semanaFin)}",
                    style = TextoCuerpo,
                    color = TextoSecundario
                )
                if (elegida) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ChipEstado(texto = "Elegida", fondo = ChipVerdeFondo, colorTexto = ChipVerdeTexto)
                }
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text(
                text = semana.precios[categoria]?.let { formatearEurosPorKilo(it) } ?: "Sin precio",
                style = TextoValor,
                color = Texto,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun RepartoPorCalidad(estado: AvanzadoUiState, viewModel: SimuladorAvanzadoViewModel) {
    Text(text = "Reparto por calidad", style = TextoValor, color = Texto)
    Text(
        text = "Qué parte de tu aceite es de cada calidad. Tiene que sumar 100 %.",
        style = TextoCuerpo,
        color = TextoSecundario
    )
    Spacer(modifier = Modifier.height(16.dp))
    CampoDeTexto(
        etiqueta = "AOVE",
        valor = estado.aove,
        alCambiar = { viewModel.cambiarReparto(CategoriaAceite.AOVE, it) },
        tipo = KeyboardType.Decimal,
        sufijo = "%"
    )
    Spacer(modifier = Modifier.height(16.dp))
    CampoDeTexto(
        etiqueta = "Virgen",
        valor = estado.virgen,
        alCambiar = { viewModel.cambiarReparto(CategoriaAceite.VIRGEN, it) },
        tipo = KeyboardType.Decimal,
        sufijo = "%"
    )
    Spacer(modifier = Modifier.height(16.dp))
    CampoDeTexto(
        etiqueta = "Lampante",
        valor = estado.lampante,
        alCambiar = { viewModel.cambiarReparto(CategoriaAceite.LAMPANTE, it) },
        tipo = KeyboardType.Decimal,
        sufijo = "%"
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (estado.avisoDeReparto.isNotEmpty()) {
        Text(text = estado.avisoDeReparto, style = TextoCuerpo, color = Error)
    } else {
        val ponderado = estado.semanaUsada?.let { precioPonderado(it.precios, estado.reparto) }
        if (ponderado != null) {
            Text(
                text = "Precio ponderado: ${formatearEurosPorKilo(ponderado)}",
                style = TextoCuerpo,
                color = TextoSecundario
            )
        }
    }
}

/** Importe bruto, gastos, importe neto y euros por kilo de aceituna, siempre estimados. */
@Composable
private fun ResultadoAvanzadoTarjeta(
    estado: AvanzadoUiState,
    aceiteNeto: Double?,
    kilos: Double?
) {
    val precio = estado.precio
    val gastos = estado.gastosPorKilo
    val resultado = if (aceiteNeto != null && kilos != null && precio != null && gastos != null) {
        calcularImporteAvanzado(aceiteNeto, kilos, precio, gastos)
    } else {
        null
    }
    Tarjeta {
        Text(
            text = "Importe estimado",
            style = TextoValor,
            color = Texto,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (resultado == null) {
            Text(
                text = mensajeSinImporte(estado, aceiteNeto, precio, gastos),
                style = TextoCuerpo,
                color = if (estado.avisoDeReparto.isNotEmpty()) Error else TextoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            FilaDato("Importe bruto estimado", formatearEuros(resultado.importeBruto))
            Spacer(modifier = Modifier.height(12.dp))
            FilaDato("Gastos de la cooperativa", "-" + formatearEuros(resultado.gastos))
            Spacer(modifier = Modifier.height(12.dp))
            FilaDato("Importe neto estimado", formatearEuros(resultado.importeNeto))
            Spacer(modifier = Modifier.height(12.dp))
            FilaDato(
                "Por kilo de aceituna",
                formatearEurosPorKilo(resultado.netoPorKiloDeAceituna, decimalesMinimos = 3)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Precio usado: ${formatearEurosPorKilo(resultado.precio)}",
                style = TextoCuerpo,
                color = TextoSecundario
            )
            if (resultado.gastosSuperanElPrecio) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Los gastos son mayores que el precio: el importe neto sale negativo.",
                    style = TextoCuerpo,
                    color = Error
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Estimación orientativa: la liquidación oficial es la de la cooperativa.",
            style = TextoCuerpo,
            color = TextoSecundario
        )
        Spacer(modifier = Modifier.height(8.dp))
        PieDePrecio(estado)
    }
}

/** Aviso de precio de referencia, fuente y semana del precio usado. */
@Composable
private fun PieDePrecio(estado: AvanzadoUiState) {
    if (estado.modo == ModoPrecio.MANUAL) {
        Text(text = "Precio escrito por ti.", style = TextoCuerpo, color = TextoSecundario)
        return
    }
    Text(text = AVISO_PRECIO_DE_REFERENCIA, style = TextoCuerpo, color = Texto)
    val semana = estado.semanaUsada
    if (estado.fuente.isNotEmpty()) {
        Text(text = estado.fuente, style = TextoCuerpo, color = TextoSecundario)
    }
    if (semana != null) {
        Text(
            text = "Semana del ${formatearFecha(semana.semana)} al " +
                "${formatearFecha(semana.semanaFin)} · ${estado.mercado.etiqueta}",
            style = TextoCuerpo,
            color = TextoSecundario
        )
    }
}

private fun mensajeSinImporte(
    estado: AvanzadoUiState,
    aceiteNeto: Double?,
    precio: Double?,
    gastos: Double?
): String = when {
    aceiteNeto == null -> "Escribe los kilos y el rendimiento para ver el importe."
    estado.avisoDeReparto.isNotEmpty() -> estado.avisoDeReparto
    precio == null || precio <= 0.0 ->
        if (estado.modo == ModoPrecio.MANUAL) {
            "Escribe el precio del aceite en €/kg."
        } else {
            "Elige una semana para ver el importe."
        }
    gastos == null || gastos < 0.0 -> "Escribe los gastos de la cooperativa en €/kg."
    else -> "Escribe los kilos y el rendimiento para ver el importe."
}

private fun textoAntiguedad(horas: Int): String = when {
    horas < 1 -> "menos de una hora"
    horas < 48 -> "$horas h"
    else -> "${horas / 24} días"
}
