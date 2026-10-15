package com.olivapp.app.core.navegacion

import com.olivapp.app.R

/**
 * Lógica pura de la barra de navegación (CONTEXT.md: "Barra de navegación") y
 * del botón atrás del sistema, separada de la interfaz para poder probarla sin
 * Compose (feature `navegacion-barra-inferior`).
 *
 * El router propio de [OlivApp] no cambia: sigue siendo `irA("pantalla")` con
 * una tienda de ViewModels por visita. Las pestañas solo cambian a qué pantalla
 * se llama, sin historial entre ellas.
 */

/** Las cinco secciones principales de la app tras iniciar sesión. */
enum class Pestana(val pantalla: String, val etiqueta: String, val icono: Int) {
    Inicio("sesion", "Inicio", R.drawable.ic_inicio),
    Cuaderno("cuaderno", "Cuaderno", R.drawable.ic_cuaderno),
    Simulador("simulador", "Simulador", R.drawable.ic_simulador),
    Parcelas("parcelas", "Parcelas", R.drawable.ic_parcelas),
    Perfil("perfil", "Perfil", R.drawable.ic_perfil)
}

/** Pantallas de primer nivel: las que llevan la barra de navegación. */
private val PANTALLAS_DE_PRIMER_NIVEL = Pestana.entries.map { it.pantalla }.toSet()

/** Pantallas de segundo nivel: ocultan la barra y conservan "Volver". */
private val PANTALLAS_DE_SEGUNDO_NIVEL = setOf("campana", "nueva_labor", "notas_voz")

/**
 * La barra de navegación se ve solo en las pantallas de primer nivel tras
 * iniciar sesión: las de segundo nivel la ocultan (conservan "Volver") y las
 * pantallas de acceso son anteriores a la sesión.
 */
fun barraInferiorVisible(pantalla: String): Boolean = pantalla in PANTALLAS_DE_PRIMER_NIVEL

/**
 * La pestaña activa de una pantalla. Las pantallas de segundo nivel
 * pertenecen a la pestaña de la que se abren (aunque su barra esté oculta).
 */
fun pestanaDe(pantalla: String): Pestana = when (pantalla) {
    "cuaderno", "nueva_labor" -> Pestana.Cuaderno
    "simulador" -> Pestana.Simulador
    "parcelas" -> Pestana.Parcelas
    "perfil" -> Pestana.Perfil
    else -> Pestana.Inicio
}

/**
 * Pantalla a la que lleva pulsar una pestaña, o `null` si ya se está en ella
 * (pulsar la pestaña activa no cambia de pantalla ni recarga nada).
 */
fun pantallaAlElegir(pestana: Pestana, pantallaActual: String): String? =
    pestana.pantalla.takeIf { it != pantallaActual }

/** Lo que hace el botón atrás del sistema en cada pantalla. */
sealed interface AccionAtras {

    /** No se consume: el sistema cierra la app (comportamiento de siempre). */
    data object Salir : AccionAtras

    /** El router propio vuelve a esa pantalla, sin historial acumulado. */
    data class IrA(val destino: String) : AccionAtras
}

/**
 * El botón atrás del sistema:
 * - desde una pestaña distinta de Inicio vuelve a Inicio;
 * - desde Inicio sale de la app;
 * - en las pantallas de segundo nivel hace lo mismo que su botón "Volver";
 * - en las pantallas de acceso no se consume (hoy sale de la app).
 *
 * No hay historial: el destino solo depende de la pantalla actual, nunca de
 * por dónde se llegue a ella.
 */
fun accionDeAtras(pantalla: String): AccionAtras = when (pantalla) {
    "sesion", "inicio", "inicio_sesion", "registro" -> AccionAtras.Salir
    "campana", "notas_voz" -> AccionAtras.IrA("sesion")
    "nueva_labor" -> AccionAtras.IrA("cuaderno")
    in PANTALLAS_DE_PRIMER_NIVEL -> AccionAtras.IrA("sesion")
    else -> AccionAtras.Salir
}
