package com.olivapp.app.voz.domain

import kotlin.math.log10

/** Amplitud máxima que devuelve `MediaRecorder.getMaxAmplitude` (16 bits con signo). */
const val AMPLITUD_MAXIMA = 32767

/** Cada cuántos milisegundos se consulta la amplitud del micrófono. */
const val INTERVALO_AMPLITUD_MS = 80L

/** Cuántas muestras de amplitud se dibujan a la vez en la onda. */
const val MUESTRAS_DE_ONDA = 24

/** Por debajo de estos decibelios (respecto al máximo) se considera silencio. */
private const val DECIBELIOS_DE_SILENCIO = -45.0

/**
 * Convierte la amplitud cruda del micrófono (0..32767) en un nivel visual de
 * 0 a 1. Se usa una escala logarítmica (decibelios) porque el oído, y la voz
 * hablada, se mueven mucho más cerca del silencio que del máximo: con una
 * escala lineal la onda casi no se vería al hablar con normalidad.
 */
fun normalizarAmplitud(amplitud: Int): Float {
    if (amplitud <= 0) return 0f
    val decibelios = 20.0 * log10(amplitud.coerceAtMost(AMPLITUD_MAXIMA).toDouble() / AMPLITUD_MAXIMA)
    val nivel = (decibelios - DECIBELIOS_DE_SILENCIO) / -DECIBELIOS_DE_SILENCIO
    return nivel.coerceIn(0.0, 1.0).toFloat()
}

/**
 * Añade un nivel nuevo al final de la onda y descarta los más antiguos para
 * que nunca haya más de [maximo] muestras. La onda avanza de derecha a izquierda.
 */
fun agregarNivelDeOnda(
    niveles: List<Float>,
    nuevo: Float,
    maximo: Int = MUESTRAS_DE_ONDA
): List<Float> = (niveles + nuevo.coerceIn(0f, 1f)).takeLast(maximo)

/** Onda en reposo: todo silencio, para dibujar antes de que llegue la primera muestra. */
fun ondaEnSilencio(muestras: Int = MUESTRAS_DE_ONDA): List<Float> = List(muestras) { 0f }

/** Segundos que le quedan a la nota antes del límite (nunca negativo). */
fun segundosRestantes(segundos: Int, limite: Int): Int = (limite - segundos).coerceAtLeast(0)

/**
 * Fracción de tiempo que queda (1 al empezar, 0 en el límite) para el anillo
 * de cuenta atrás. Acepta segundos con decimales para animar el anillo.
 */
fun fraccionRestante(segundos: Float, limite: Int): Float {
    if (limite <= 0) return 0f
    return (1f - segundos / limite).coerceIn(0f, 1f)
}
