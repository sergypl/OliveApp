package com.olivapp.app.acceso.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.olivapp.app.acceso.domain.SesionRepositorio

/**
 * Almacenamiento seguro del token de sesión: preferencias cifradas con una
 * clave del Keystore del dispositivo (AES-256).
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
        preferencias.edit().putString("token", token).apply()
    }

    override fun token(): String? = preferencias.getString("token", null)

    override fun hayToken(): Boolean = token() != null

    override fun borrar() {
        preferencias.edit().remove("token").apply()
    }
}
