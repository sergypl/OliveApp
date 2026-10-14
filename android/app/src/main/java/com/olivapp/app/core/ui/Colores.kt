package com.olivapp.app.core.ui

import androidx.compose.ui.graphics.Color

// Sistema visual "Agro-Modern Olive Systems" (fuente de verdad: DESIGN.md y
// docs/design/stitch/). Todos los pares texto/fondo cumplen WCAG AA; los
// márgenes de contraste están calculados en la Fase 0 de
// rediseno-visual-android.

// Primario: verde oliva del logo (botones principales y cabeceras de sección).
internal val Primario = Color(0xFF2D5A27)
internal val PrimarioPulsado = Color(0xFF154212)
internal val SobrePrimario = Color(0xFFFFFFFF)

// Acento: naranja de cosecha, reservado al micrófono y a la voz. Su contenido
// va en marrón oscuro: el blanco sobre este naranja no pasa AA (2,2:1).
internal val Acento = Color(0xFFFE932C)
internal val SobreAcento = Color(0xFF663500)

// Superficies: canvas, tarjetas y paneles destacados (tokens Stitch literales).
internal val Fondo = Color(0xFFFAF8FF)
internal val Superficie = Color(0xFFFFFFFF)
internal val PanelDestacado = Color(0xFFEAEDFF)
internal val PanelDestacadoFuerte = Color(0xFFE2E7FF)
internal val Borde = Color(0xFFE2E8F0)

// Texto.
internal val Texto = Color(0xFF131B2E)
internal val TextoSecundario = Color(0xFF42493E)

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

// Alias legados para las pantallas aún no rediseñadas: cada fase del rediseño
// los sustituye por los tokens nuevos y al final se retiran.
internal val FondoCrema = Fondo
internal val TextoMarron = Texto
internal val VerdeOliva = Primario
internal val TextoError = Error
