package com.olivapp.app.core.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.olivapp.app.R

// Tipografía empaquetada (ADR 0009): Montserrat para títulos y cifras, Inter
// para cuerpo y controles. Tamaños escalados a la regla sénior de DESIGN.md:
// cuerpo 18sp y nada por debajo de 16sp, aunque los mockups de Stitch usen
// cuerpos de 12-16px.
internal val Titulos = FontFamily(
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold)
)

internal val Cuerpo = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

// Título de pantalla.
internal val TextoTitulo1 = TextStyle(
    fontFamily = Titulos,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 36.sp
)

// Título de sección.
internal val TextoTitulo2 = TextStyle(
    fontFamily = Titulos,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp
)

// Cifra destacada (resultado del simulador).
internal val TextoCifra = TextStyle(
    fontFamily = Titulos,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    lineHeight = 48.sp
)

// Valor destacado de un dato (etiqueta/valor).
internal val TextoValor = TextStyle(
    fontFamily = Titulos,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp
)

// Cuerpo y etiquetas de formulario.
internal val TextoCuerpo = TextStyle(
    fontFamily = Cuerpo,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 26.sp
)

// Etiqueta corta (chips, subtítulos): nunca por debajo de 16sp.
internal val TextoEtiqueta = TextStyle(
    fontFamily = Cuerpo,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp
)

// Texto de botón grande.
internal val TextoBoton = TextStyle(
    fontFamily = Cuerpo,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 24.sp
)
