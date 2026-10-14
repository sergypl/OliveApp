package com.olivapp.app.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Sistema de diseño de OlivApp en un único sitio (rediseño visual): colores,
// tipografía, formas y tema Material 3. Las pantallas leen de aquí y de los
// componentes de core/ui; no reparten estilos por su cuenta.

internal val FormasOlivApp = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val ColoresOlivApp = lightColorScheme(
    primary = Primario,
    onPrimary = SobrePrimario,
    primaryContainer = Primario,
    onPrimaryContainer = SobrePrimario,
    secondary = Acento,
    onSecondary = SobreAcento,
    secondaryContainer = Acento,
    onSecondaryContainer = SobreAcento,
    background = Fondo,
    onBackground = Texto,
    surface = Superficie,
    onSurface = Texto,
    surfaceVariant = PanelDestacado,
    onSurfaceVariant = TextoSecundario,
    outline = TextoSecundario,
    error = Error,
    onError = Color.White,
    errorContainer = FondoErrorSuave,
    onErrorContainer = SobreErrorSuave
)

private val TipografiaOlivApp = Typography(
    displayLarge = TextoCifra,
    headlineLarge = TextoTitulo1,
    headlineMedium = TextoTitulo2,
    headlineSmall = TextoValor,
    titleLarge = TextoTitulo1,
    titleMedium = TextoTitulo2,
    bodyLarge = TextoCuerpo,
    bodyMedium = TextoCuerpo,
    labelLarge = TextoBoton,
    labelMedium = TextoEtiqueta,
    labelSmall = TextoEtiqueta
)

@Composable
fun OlivAppTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColoresOlivApp,
        typography = TipografiaOlivApp,
        shapes = FormasOlivApp,
        content = contenido
    )
}
