package com.olivapp.app.acceso.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.olivapp.app.acceso.domain.SesionRepositorio

/**
 * Almacenamiento seguro del token de sesión: preferencias cifradas con una
 * clave del Keystore del dispositivo (AES-256).
 *
 * Junto al token se guarda la identidad de la cuenta (la reclamación "sub" del
 * JWT, ver [idDeCuentaDe]): es la que amarra a su dueño la cola de notas de voz
 * del teléfono, que es común al dispositivo.
 */
class AlmacenSesiones(contexto: Context) : SesionRepositorio {
    private val preferencias = EncryptedSharedPreferences.create(
        contexto,
        "olivapp-sesiones",
        MasterKey.Builder(contexto)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun guardar(token: String) {
        preferencias.edit()
            .putString("token", token)
            .putString("id_cuenta", idDeCuentaDe(token))
            .apply()
    }

    override fun token(): String? = preferencias.getString("token", null)

    /**
     * Identidad de la cuenta de la sesión activa (el id de cuenta del token).
     * Con un token guardado antes de que existiera esta identidad se recupera
     * de él y se guarda, para que la cuenta no se quede sin dueño.
     */
    fun idDeCuenta(): String? =
        preferencias.getString("id_cuenta", null)
            ?: idDeCuentaDe(token())?.also { preferencias.edit().putString("id_cuenta", it).apply() }

    override fun hayToken(): Boolean = token() != null

    override fun borrar() {
        preferencias.edit().remove("token").remove("id_cuenta").apply()
    }
}
