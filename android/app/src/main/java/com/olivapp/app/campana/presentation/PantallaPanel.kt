package com.olivapp.app.campana.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olivapp.app.R
import com.olivapp.app.campana.domain.BarraDeAceite
import com.olivapp.app.campana.domain.ComparativaAceite
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.DatosPanel
import com.olivapp.app.campana.domain.saludoDeCabecera
import com.olivapp.app.core.formatos.formatearCantidad
import com.olivapp.app.core.ui.AlturaBarraInferior
import com.olivapp.app.core.ui.BotonMicrofonoFlotante
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.Borde
import com.olivapp.app.core.ui.ChipEstado
import com.olivapp.app.core.ui.ChipVerdeFondo
import com.olivapp.app.core.ui.ChipVerdeTexto
import com.olivapp.app.core.ui.ContenedorBajo
import com.olivapp.app.core.ui.ContenedorRefrescar
import com.olivapp.app.core.ui.EsqueletoDeCarga
import com.olivapp.app.core.ui.Error
import com.olivapp.app.core.ui.EstadoDeError
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.FormasOlivApp
import com.olivapp.app.core.ui.LocalBarraInferiorVisible
import com.olivapp.app.core.ui.Primario
import com.olivapp.app.core.ui.SobrePrimario
import com.olivapp.app.core.ui.Superficie
import com.olivapp.app.core.ui.Tarjeta
import com.olivapp.app.core.ui.TarjetaDatoVariacion
import com.olivapp.app.core.ui.Texto
import com.olivapp.app.core.ui.TextoCifra
import com.olivapp.app.core.ui.TextoCuerpo
import com.olivapp.app.core.ui.TextoSecundario
import com.olivapp.app.core.ui.TextoTitulo1
import com.olivapp.app.core.ui.TextoTitulo2
import com.olivapp.app.core.ui.TextoValor
import com.olivapp.app.core.ui.pantallaCompleta
import com.olivapp.app.perfil.domain.DatosPerfil
import com.olivapp.app.perfil.domain.inicialesDe

/** Espacio al final del desplazamiento para que el botón flotante no tape la última fila. */
private val HuecoDelBotonFlotante = 96.dp

/**
 * Panel (pestaña Inicio): cabecera con saludo, cooperativa y avatar que abre
 * el Perfil; tarjeta destacada de la campaña actual (datos oficiales);
 * comparativa con la campaña anterior; campañas anteriores como lista y el
 * botón flotante "Dictar nota". Se refresca deslizando hacia abajo.
 */
@Composable
fun PantallaPanel(
    onAbrirNotasVoz: () -> Unit,
    onAbrirCampana: (String) -> Unit,
    onAbrirPerfil: () -> Unit,
    viewModel: PanelViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Carga autenticada: el token guardado acompaña a las peticiones.
    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    val hora = remember { java.time.LocalTime.now().hour }
    val datos = estado.datos

    Box(modifier = Modifier.fillMaxSize()) {
        ContenedorRefrescar(
            refrescando = estado.refrescando,
            alRefrescar = viewModel::alRefrescar,
            modificador = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .pantallaCompleta()
                    .verticalScroll(rememberScrollState())
            ) {
                CabeceraDelPanel(
                    saludo = saludoDeCabecera(hora, estado.perfil?.codigoSocio),
                    perfil = estado.perfil,
                    alAbrirPerfil = onAbrirPerfil
                )
                Spacer(modifier = Modifier.height(24.dp))
                when {
                    datos != null -> {
                        TarjetaCampanaActual(datos)
                        estado.mensaje?.let { mensaje ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = mensaje, style = TextoCuerpo, color = Error)
                        }
                    }
                    // Falló la carga y no hay nada que enseñar: es un error, no "sin datos".
                    estado.mensaje != null -> EstadoDeError(
                        titulo = if (estado.sinConexion) "Sin conexión" else "No se pudieron cargar tus datos",
                        mensaje = estado.mensaje ?: "",
                        sinConexion = estado.sinConexion,
                        alReintentar = viewModel::cargar
                    )
                    else -> {
                        Text(
                            text = "Cargando tus datos…",
                            style = TextoCuerpo,
                            color = TextoSecundario
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        EsqueletoDeCarga(filas = 1, altoFila = 220.dp)
                        EsqueletoDeCarga(filas = 3, altoFila = 56.dp)
                    }
                }
                estado.comparativa?.let { comparativa ->
                    Spacer(modifier = Modifier.height(32.dp))
                    SeccionComparativa(comparativa, estado.barras)
                }
                val anteriores = estado.campanasAnteriores
                if (anteriores.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    ListaDeCampanasAnteriores(anteriores, onAbrirCampana)
                } else if (estado.errorDeCampanas != null && datos != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    ErrorDeCampanas(estado.errorDeCampanas ?: "", viewModel::cargar)
                }
                Spacer(modifier = Modifier.height(HuecoDelBotonFlotante))
            }
        }
        // El micrófono es el protagonista del panel: botón flotante naranja
        // sobre la barra inferior, en la esquina de la mano que pulsa.
        val huecoDeLaBarra = if (LocalBarraInferiorVisible.current) AlturaBarraInferior else 0.dp
        BotonMicrofonoFlotante(
            alPulsar = onAbrirNotasVoz,
            modificador = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Bottom)
                )
                .padding(end = 24.dp, bottom = huecoDeLaBarra + 16.dp)
        )
    }
}

/** Saludo según la hora, cooperativa debajo y avatar de iniciales que abre el Perfil. */
@Composable
private fun CabeceraDelPanel(saludo: String, perfil: DatosPerfil?, alAbrirPerfil: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = saludo, style = TextoTitulo1, color = Texto)
            if (perfil != null && perfil.cooperativa.isNotBlank()) {
                Text(text = perfil.cooperativa, style = TextoCuerpo, color = TextoSecundario)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        // Atajo a la pestaña Perfil (que también está en la barra inferior, con texto).
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Primario)
                .clickable(onClickLabel = "Abrir tu perfil", role = Role.Button, onClick = alAbrirPerfil)
                .semantics { contentDescription = "Tu perfil" },
            contentAlignment = Alignment.Center
        ) {
            if (perfil != null) {
                Text(
                    text = inicialesDe(perfil.email),
                    style = TextoValor,
                    color = SobrePrimario,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_perfil),
                    contentDescription = null,
                    tint = SobrePrimario,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Tarjeta destacada de la campaña actual: el aceite obtenido como cifra
 * grande, kilos y rendimiento como datos secundarios y la etiqueta de dato
 * oficial. Los datos oficiales no se mezclan con las labores del socio.
 */
@Composable
private fun TarjetaCampanaActual(datos: DatosPanel) {
    Tarjeta {
        Text(text = "Campaña ${datos.campana}", style = TextoTitulo2, color = Texto)
        Spacer(modifier = Modifier.height(8.dp))
        ChipEstado(texto = "Oficial · Cooperativa", fondo = ChipVerdeFondo, colorTexto = ChipVerdeTexto)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Aceite obtenido", style = TextoCuerpo, color = TextoSecundario)
        Text(text = datos.aceiteObtenido, style = TextoCifra, color = Primario)
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Borde)
        Spacer(modifier = Modifier.height(16.dp))
        FilaDato(etiqueta = "Kilos entregados", valor = datos.kilosEntregados, icono = R.drawable.ic_kilos)
        Spacer(modifier = Modifier.height(12.dp))
        FilaDato(etiqueta = "Rendimiento graso", valor = datos.rendimientoGraso, icono = R.drawable.ic_rendimiento)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Estos son los datos que declara la cooperativa. " +
                "Tus labores van aparte, en el cuaderno de campo.",
            style = TextoCuerpo,
            color = TextoSecundario
        )
    }
}

/** Comparativa del aceite con la campaña anterior y gráfico de barras de las campañas. */
@Composable
private fun SeccionComparativa(comparativa: ComparativaAceite, barras: List<BarraDeAceite>) {
    Text(text = "Comparativa con la campaña anterior", style = TextoTitulo2, color = Texto)
    Spacer(modifier = Modifier.height(12.dp))
    val anterior = formatearCantidad(comparativa.aceiteAnteriorKg) + " kg"
    TarjetaDatoVariacion(
        etiqueta = "Aceite obtenido",
        valor = formatearCantidad(comparativa.aceiteActualKg) + " kg",
        variacion = comparativa.textoVariacion?.takeIf { comparativa.sube != null },
        sube = comparativa.sube,
        referencia = when {
            comparativa.variacion == null ->
                "La campaña ${comparativa.etiquetaAnterior} no tiene aceite para comparar ($anterior)."
            comparativa.sube == null ->
                "Igual que la campaña ${comparativa.etiquetaAnterior} ($anterior)."
            else -> "Frente a la campaña ${comparativa.etiquetaAnterior} ($anterior)."
        }
    )
    if (barras.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        GraficoDeAceite(barras)
    }
}

/**
 * Gráfico de barras horizontales del aceite obtenido por campaña. Cada barra
 * lleva su campaña y su valor escritos, y la campaña actual se distingue por
 * el texto y por la barra rellena (las anteriores solo van con contorno), así
 * que el color no es la única señal.
 */
@Composable
private fun GraficoDeAceite(barras: List<BarraDeAceite>) {
    val maximo = barras.maxOf { it.aceiteKg }
    Tarjeta {
        Text(text = "Aceite obtenido por campaña", style = TextoValor, color = Texto)
        for (barra in barras) {
            Spacer(modifier = Modifier.height(16.dp))
            val valor = formatearCantidad(barra.aceiteKg) + " kg"
            val nombre = "Campaña ${barra.campana}" + if (barra.esActual) " (actual)" else ""
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "$nombre: $valor de aceite"
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = nombre,
                        style = TextoCuerpo,
                        color = Texto,
                        fontWeight = if (barra.esActual) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = valor, style = TextoValor, color = Texto)
                }
                Spacer(modifier = Modifier.height(6.dp))
                BarraHorizontal(
                    fraccion = if (maximo > 0.0) (barra.aceiteKg / maximo).toFloat() else 0f,
                    rellena = barra.esActual
                )
            }
        }
    }
}

@Composable
private fun BarraHorizontal(fraccion: Float, rellena: Boolean) {
    val forma = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(forma)
            .background(ContenedorBajo)
    ) {
        if (fraccion > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraccion.coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .clip(forma)
                    .then(
                        if (rellena) {
                            Modifier.background(Primario)
                        } else {
                            Modifier.background(Superficie).border(3.dp, Primario, forma)
                        }
                    )
            )
        }
    }
}

/** Campañas anteriores como filas de lista (nombre, aceite obtenido y flecha). */
@Composable
private fun ListaDeCampanasAnteriores(
    campanas: List<DatosCampanaResumen>,
    alAbrirCampana: (String) -> Unit
) {
    Text(text = "Campañas anteriores", style = TextoTitulo2, color = Texto)
    Spacer(modifier = Modifier.height(12.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FormasOlivApp.large,
        color = Superficie,
        border = BorderStroke(1.dp, Borde)
    ) {
        Column {
            campanas.forEachIndexed { indice, campana ->
                if (indice > 0) HorizontalDivider(color = Borde)
                FilaDeCampana(campana, alAbrirCampana)
            }
        }
    }
}

@Composable
private fun FilaDeCampana(campana: DatosCampanaResumen, alAbrir: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(
                onClickLabel = "Abrir la campaña ${campana.nombre}",
                role = Role.Button,
                onClick = { alAbrir(campana.id) }
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Campaña ${campana.nombre}", style = TextoValor, color = Texto)
            campana.aceiteKg?.let {
                Text(
                    text = "Aceite obtenido: ${formatearCantidad(it)} kg",
                    style = TextoCuerpo,
                    color = TextoSecundario
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_abrir),
            contentDescription = null,
            tint = TextoSecundario,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ErrorDeCampanas(mensaje: String, alReintentar: () -> Unit) {
    Text(text = "Campañas anteriores", style = TextoTitulo2, color = Texto)
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = mensaje, style = TextoCuerpo, color = Error, textAlign = TextAlign.Start)
    Spacer(modifier = Modifier.height(12.dp))
    BotonSecundario(texto = "Reintentar", icono = R.drawable.ic_refrescar, alPulsar = alReintentar)
}
