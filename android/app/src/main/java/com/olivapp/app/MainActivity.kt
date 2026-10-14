package com.olivapp.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.olivapp.app.acceso.data.AlmacenSesiones
import com.olivapp.app.campana.domain.DatosCampanaResumen
import com.olivapp.app.campana.domain.idCampanaDeLaFecha
import com.olivapp.app.parcelas.domain.DatosParcela
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.olivapp.app.core.formatos.formatearCantidad
import com.olivapp.app.core.navegacion.OlivApp
import com.olivapp.app.core.red.ServicioCuentas
import com.olivapp.app.core.ui.BotonPeligro
import com.olivapp.app.core.ui.BotonPrincipal
import com.olivapp.app.core.ui.BotonSecundario
import com.olivapp.app.core.ui.CampoDeTexto
import com.olivapp.app.core.ui.Desplegable
import com.olivapp.app.core.ui.FilaDato
import com.olivapp.app.core.ui.FondoCrema
import com.olivapp.app.core.ui.OpcionCategoria
import com.olivapp.app.core.ui.TextoError
import com.olivapp.app.core.ui.TextoMarron
import com.olivapp.app.core.ui.VerdeOliva
import com.olivapp.app.voz.data.AlmacenNotasVoz
import com.olivapp.app.voz.data.GrabadorNotas
import com.olivapp.app.voz.data.SincronizadorNotas
import com.olivapp.app.voz.domain.aplicarTranscripcion
import com.olivapp.app.voz.domain.camposFaltantesDeBorrador
import com.olivapp.app.voz.domain.etiquetaDeCampo
import com.olivapp.app.voz.domain.fechaIsoDeBorrador
import com.olivapp.app.voz.domain.lineasDeBorrador
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OlivApp() }
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
fun PantallaSimulador(onVolver: () -> Unit) {
    var kilos by remember { mutableStateOf("") }
    var rendimiento by remember { mutableStateOf("") }
    var ajuste by remember { mutableStateOf("10") }
    var resultado by remember { mutableStateOf<ResultadoSimulacion?>(null) }
    var error by remember { mutableStateOf("") }

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
            valor = kilos,
            alCambiar = {
                kilos = it
                resultado = null
            },
            tipo = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Rendimiento graso (%)",
            valor = rendimiento,
            alCambiar = {
                rendimiento = it
                resultado = null
            },
            tipo = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(16.dp))
        CampoDeTexto(
            etiqueta = "Ajuste industrial (%)",
            valor = ajuste,
            alCambiar = {
                ajuste = it
                // Recálculo al instante: si ya hay resultado y el nuevo ajuste
                // es un porcentaje válido, el aceite neto se recalcula con él.
                val ajusteNuevo = numeroDeCampo(it)
                val anterior = resultado
                if (anterior != null && ajusteNuevo != null &&
                    ajusteNuevo >= 0.0 && ajusteNuevo <= 100.0
                ) {
                    resultado = recalcularConAjuste(anterior, ajusteNuevo)
                }
            },
            tipo = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonPrincipal(
            texto = "Simular",
            alPulsar = {
                error = ""
                val kilosSimulados = numeroDeCampo(kilos)
                val rendimientoSimulado = numeroDeCampo(rendimiento)
                val ajusteSimulado = numeroDeCampo(ajuste)
                when {
                    kilosSimulados == null || kilosSimulados <= 0.0 -> {
                        resultado = null
                        error = "Introduce los kilos de aceituna, mayores que cero."
                    }
                    rendimientoSimulado == null ||
                        rendimientoSimulado <= 0.0 || rendimientoSimulado > 100.0 -> {
                        resultado = null
                        error = "Introduce el rendimiento graso como porcentaje " +
                            "entre 0 y 100 (por ejemplo, 20)."
                    }
                    ajusteSimulado == null ||
                        ajusteSimulado < 0.0 || ajusteSimulado > 100.0 -> {
                        resultado = null
                        error = "Introduce el ajuste industrial como porcentaje " +
                            "entre 0 y 100 (por ejemplo, 10)."
                    }
                    else -> resultado =
                        simularLiquidacion(
                            kilosSimulados, rendimientoSimulado, ajusteSimulado
                        )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (error.isNotEmpty()) {
            Text(
                text = error,
                fontSize = 20.sp,
                color = TextoError,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        resultado?.let {
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

/** Resultado de una simulación de liquidación en limpio (en kilos de aceite). */
class ResultadoSimulacion(
    val aceiteSegunRendimiento: Double,
    val ajustePorcentaje: Double,
    val ajusteIndustrial: Double,
    val aceiteNeto: Double
)

/**
 * Liquidación en limpio: kilos × rendimiento graso − ajuste industrial (única
 * deducción del MVP). El ajuste se expresa en porcentaje (10 % por defecto,
 * ADR 0007) y es configurable por el socio. No se guarda en ningún sitio.
 */
fun simularLiquidacion(
    kilos: Double,
    rendimiento: Double,
    ajustePorcentaje: Double = 10.0
): ResultadoSimulacion {
    val aceite = kilos * rendimiento / 100.0
    val merma = aceite * ajustePorcentaje / 100.0
    return ResultadoSimulacion(aceite, ajustePorcentaje, merma, aceite - merma)
}

/**
 * Recálculo al instante del resultado con un ajuste industrial nuevo: el aceite
 * según el rendimiento no cambia, solo la deducción que se le aplica.
 */
fun recalcularConAjuste(
    anterior: ResultadoSimulacion,
    ajustePorcentaje: Double
): ResultadoSimulacion {
    val merma = anterior.aceiteSegunRendimiento * ajustePorcentaje / 100.0
    return ResultadoSimulacion(
        anterior.aceiteSegunRendimiento,
        ajustePorcentaje,
        merma,
        anterior.aceiteSegunRendimiento - merma
    )
}

/**
 * Número tecleado por el socio: acepta coma o punto decimal ("20,5" o "20.5")
 * y los puntos de millar ("10.958"). Vacío o ilegible devuelve null.
 */
fun numeroDeCampo(texto: String): Double? {
    val limpio = texto.trim().replace(" ", "")
    if (limpio.isEmpty()) return null
    val normalizado = if (limpio.contains(',')) {
        limpio.replace(".", "").replace(',', '.')
    } else {
        val partes = limpio.split('.')
        if (partes.size > 1 && partes.last().length == 3) {
            limpio.replace(".", "")
        } else {
            limpio
        }
    }
    return normalizado.toDoubleOrNull()
}







