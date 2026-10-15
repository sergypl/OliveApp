package com.olivapp.app.simulador.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.olivapp.app.simulador.domain.PreferenciasSimulador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.almacenSimulador by preferencesDataStore(name = "olivapp-simulador")

/**
 * Preferencias del simulador en DataStore. La lectura inicial es única y
 * pequeña (un booleano) y queda en memoria; la escritura va en segundo plano.
 */
class PreferenciasSimuladorDataStore(contexto: Context) : PreferenciasSimulador {

    private val almacen = contexto.applicationContext.almacenSimulador
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var avanzado: Boolean? = null

    override fun avanzadoActivado(): Boolean {
        avanzado?.let { return it }
        val leido = runBlocking { almacen.data.first()[CLAVE_AVANZADO] ?: false }
        avanzado = leido
        return leido
    }

    override fun guardarAvanzadoActivado(activado: Boolean) {
        avanzado = activado
        ambito.launch { almacen.edit { it[CLAVE_AVANZADO] = activado } }
    }

    private companion object {
        val CLAVE_AVANZADO = booleanPreferencesKey("simulador_avanzado")
    }
}
