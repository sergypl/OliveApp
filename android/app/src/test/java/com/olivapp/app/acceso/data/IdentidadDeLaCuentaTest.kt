package com.olivapp.app.acceso.data

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Con Robolectric como el resto de la batería: el cuerpo del token es JSON
// (org.json), que en JVM sin Robolectric no está implementado.
@RunWith(RobolectricTestRunner::class)
/**
 * Tests de la identidad de la cuenta que lleva el token de sesión (feature
 * aislamiento-de-cola-por-socio): la cola de notas del teléfono se amarra a
 * esta identidad, la misma para un socio en cada inicio de sesión.
 */
class IdentidadDeLaCuentaTest {

    private fun tokenCon(cuerpo: String): String {
        fun codificar(texto: String) =
            Base64.getUrlEncoder().withoutPadding()
                .encodeToString(texto.toByteArray(Charsets.UTF_8))
        return codificar("""{"alg":"HS256"}""") + "." + codificar(cuerpo) + "." + codificar("firma")
    }

    @Test
    fun `el token del backend lleva el id de la cuenta`() {
        val token = tokenCon("""{"sub":"cuenta-1","email":"ana@example.com","exp":999}""")

        assertEquals("cuenta-1", idDeCuentaDe(token))
    }

    @Test
    fun `el mismo socio en dos sesiones sigue siendo la misma cuenta`() {
        val sesion1 = tokenCon("""{"sub":"cuenta-1","exp":1000}""")
        val sesion2 = tokenCon("""{"sub":"cuenta-1","exp":2000}""")

        assertEquals(idDeCuentaDe(sesion1), idDeCuentaDe(sesion2))
    }

    @Test
    fun `dos cuentas distintas nunca se confunden`() {
        val ana = tokenCon("""{"sub":"cuenta-ana","exp":1000}""")
        val luis = tokenCon("""{"sub":"cuenta-luis","exp":1000}""")

        assertNotEquals(idDeCuentaDe(ana), idDeCuentaDe(luis))
    }

    @Test
    fun `un token que no es un JWT no da identidad de cuenta`() {
        assertNull(idDeCuentaDe("token-abc"))
        assertNull(idDeCuentaDe("una.dos"))
        assertNull(idDeCuentaDe("una.dos.tres.cuatro"))
        assertNull(idDeCuentaDe("esto-no-es-base64.aQuien.no"))
        assertNull(idDeCuentaDe(""))
        assertNull(idDeCuentaDe(null))
    }

    @Test
    fun `un token sin la reclamacion sub no da identidad de cuenta`() {
        assertNull(idDeCuentaDe(tokenCon("""{"email":"ana@example.com"}""")))
        assertNull(idDeCuentaDe(tokenCon("""{"sub":""}""")))
    }
}
