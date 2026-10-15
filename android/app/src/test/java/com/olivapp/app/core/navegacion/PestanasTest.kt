package com.olivapp.app.core.navegacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la lógica pura de la barra de navegación y del botón atrás
 * (feature navegacion-barra-inferior).
 */
class PestanasTest {

    @Test
    fun `las pestanas van en el orden Inicio Cuaderno Simulador Parcelas Perfil`() {
        assertEquals(
            listOf("Inicio", "Cuaderno", "Simulador", "Parcelas", "Perfil"),
            Pestana.entries.map { it.etiqueta }
        )
    }

    @Test
    fun `la barra se ve solo en las pantallas de primer nivel`() {
        for (pestana in Pestana.entries) {
            assertTrue(pestana.pantalla, barraInferiorVisible(pestana.pantalla))
        }
    }

    @Test
    fun `la barra se oculta en segundo nivel y en las pantallas de acceso`() {
        for (pantalla in listOf("campana", "nueva_labor", "notas_voz", "inicio", "inicio_sesion", "registro")) {
            assertFalse(pantalla, barraInferiorVisible(pantalla))
        }
    }

    @Test
    fun `cada pantalla de primer nivel marca su pestana`() {
        for (pestana in Pestana.entries) {
            assertEquals(pestana, pestanaDe(pestana.pantalla))
        }
    }

    @Test
    fun `las pantallas de segundo nivel pertenecen a la pestana de la que se abren`() {
        assertEquals(Pestana.Cuaderno, pestanaDe("nueva_labor"))
        assertEquals(Pestana.Inicio, pestanaDe("campana"))
        assertEquals(Pestana.Inicio, pestanaDe("notas_voz"))
    }

    @Test
    fun `pulsar otra pestana lleva a su pantalla`() {
        assertEquals("cuaderno", pantallaAlElegir(Pestana.Cuaderno, "sesion"))
        assertEquals("sesion", pantallaAlElegir(Pestana.Inicio, "perfil"))
        assertEquals("perfil", pantallaAlElegir(Pestana.Perfil, "parcelas"))
    }

    @Test
    fun `pulsar la pestana activa no cambia de pantalla`() {
        for (pestana in Pestana.entries) {
            assertNull(pestana.pantalla, pantallaAlElegir(pestana, pestana.pantalla))
        }
    }

    @Test
    fun `atras desde una pestana distinta de Inicio vuelve a Inicio`() {
        for (pestana in Pestana.entries.filter { it != Pestana.Inicio }) {
            assertEquals(pestana.pantalla, AccionAtras.IrA("sesion"), accionDeAtras(pestana.pantalla))
        }
    }

    @Test
    fun `atras desde Inicio sale de la app`() {
        assertEquals(AccionAtras.Salir, accionDeAtras("sesion"))
    }

    @Test
    fun `atras en las pantallas de acceso sale de la app como siempre`() {
        for (pantalla in listOf("inicio", "inicio_sesion", "registro")) {
            assertEquals(pantalla, AccionAtras.Salir, accionDeAtras(pantalla))
        }
    }

    @Test
    fun `atras en segundo nivel hace lo mismo que su boton Volver`() {
        assertEquals(AccionAtras.IrA("sesion"), accionDeAtras("campana"))
        assertEquals(AccionAtras.IrA("sesion"), accionDeAtras("notas_voz"))
        assertEquals(AccionAtras.IrA("cuaderno"), accionDeAtras("nueva_labor"))
    }

    @Test
    fun `no hay historial acumulado entre pestanas`() {
        // Recorrer pestañas y pulsar atrás siempre cae en Inicio en un solo paso.
        var pantalla = "sesion"
        for (p in listOf(Pestana.Parcelas, Pestana.Simulador, Pestana.Perfil)) {
            pantalla = pantallaAlElegir(p, pantalla) ?: pantalla
        }
        assertEquals("perfil", pantalla)
        val atras = accionDeAtras(pantalla) as AccionAtras.IrA
        assertEquals(AccionAtras.Salir, accionDeAtras(atras.destino))
    }
}
