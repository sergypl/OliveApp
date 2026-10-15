package com.olivapp.app

import com.olivapp.app.campana.domain.idCampanaDeLaFecha
import com.olivapp.app.voz.domain.fechaVisible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests de caracterización de la regla de campaña oleícola (1 de noviembre a 31
 * de octubre, misma regla que el backend en `labores.campana_de_la_fecha`) y
 * del formato visible de las fechas.
 */
class CampanaCaracterizacionTest {

    @Test
    fun `las fechas de la campana 2025-2026 caen en ella`() {
        assertEquals("2025-2026", idCampanaDeLaFecha("2025-11-01"))
        assertEquals("2025-2026", idCampanaDeLaFecha("2026-01-20"))
        assertEquals("2025-2026", idCampanaDeLaFecha("2026-10-31"))
    }

    @Test
    fun `el 1 de noviembre empieza la campana siguiente`() {
        assertEquals("2026-2027", idCampanaDeLaFecha("2026-11-01"))
    }

    @Test
    fun `el 31 de octubre cierra la campana anterior`() {
        assertEquals("2024-2025", idCampanaDeLaFecha("2025-10-31"))
    }

    @Test
    fun `una fecha imposible no da campana en vez de reventar`() {
        // La fecha viene del borrador IA y solo está validada de forma:
        // "2026-02-30" pasa ese filtro pero no existe como fecha.
        assertNull(idCampanaDeLaFecha("2026-02-30"))
        assertNull(idCampanaDeLaFecha("2026-13-01"))
        assertNull(idCampanaDeLaFecha("no-es-una-fecha"))
        assertNull(idCampanaDeLaFecha(""))
    }

    @Test
    fun `la fecha ISO se lee en formato espanol`() {
        assertEquals("20/01/2026", fechaVisible("2026-01-20"))
    }

    @Test
    fun `la fecha sin dia concreto se deja tal cual`() {
        assertEquals("hoy", fechaVisible("hoy"))
        assertEquals("la semana pasada", fechaVisible("la semana pasada"))
    }
}
