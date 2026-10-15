package com.olivapp.app.core.ui

import androidx.compose.ui.graphics.Color

// Sistema visual "Agro-Modern Olive Systems" (fuente de verdad: DESIGN.md y
// docs/design/stitch/), con la paleta de superficies ya en neutros cálidos
// derivados del verde oliva (#2D5A27) de fundamentos-visuales-modernos: se
// retiró el tinte lavanda/azulado de los tokens Stitch (#FAF8FF, #EAEDFF y
// #E2E7FF). Los pares de contraste AA están calculados en DESIGN.md.

// Primario: verde oliva del logo (botones principales y cabeceras de sección).
internal val Primario = Color(0xFF2D5A27)
internal val PrimarioPulsado = Color(0xFF154212)
internal val SobrePrimario = Color(0xFFFFFFFF)

// Acento: naranja de cosecha, reservado AL MICRÓFONO y a las acciones de voz.
// Su contenido va en marrón oscuro: el blanco sobre este naranja no pasa AA
// (2,2:1) y este par sí (4,5:1).
internal val Acento = Color(0xFFFE932C)
internal val SobreAcento = Color(0xFF663500)

// Escala tonal de superficies derivada del oliva (M3: surface y los
// surfaceContainer de Lowest a Highest). Superficies neutras cálidas, sin
// tinte lavanda.
internal val Fondo = Color(0xFFF4F8F0)              // background (tono 98)
internal val Superficie = Color(0xFFFDFFFC)         // surface (tono 100)
internal val ContenedorMasBajo = Color(0xFFFDFFFA)  // surfaceContainerLowest
internal val ContenedorBajo = Color(0xFFEEF2E9)     // surfaceContainerLow
internal val Contenedor = Color(0xFFE7EBE2)         // surfaceContainer
internal val ContenedorAlto = Color(0xFFE0E5DA)     // surfaceContainerHigh
internal val ContenedorMasAlto = Color(0xFFD9DFD3)  // surfaceContainerHighest
internal val Borde = Color(0xFFC1C6BA)              // outlineVariant (tono 82)

// Texto.
internal val Texto = Color(0xFF18200E)              // onSurface (tono 20)
internal val TextoSecundario = Color(0xFF46503A)    // onSurfaceVariant (tono 38)

// Error y acciones destructivas.
internal val Error = Color(0xFFBA1A1A)
internal val SobreErrorSuave = Color(0xFF93000A)
internal val FondoErrorSuave = Color(0xFFFFDAD6)

// Chips de estado (siempre con etiqueta de texto, nunca solo color).
internal val ChipVerdeFondo = Color(0xFFDCFCE7)
internal val ChipVerdeTexto = Color(0xFF166534)
internal val ChipAmbarFondo = Color(0xFFFEF3C7)
internal val ChipAmbarTexto = Color(0xFF92400E)
internal val ChipRojoFondo = Color(0xFFFEE2E2)
internal val ChipRojoTexto = Color(0xFF991B1B)
