package com.olivapp.app.simulador.data

import android.content.Context
import com.olivapp.app.simulador.domain.PreciosAceiteRepositorio
import com.olivapp.app.simulador.domain.PreferenciasSimulador
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Precios de referencia y preferencias del simulador avanzado (ADR 0008: DI con Hilt). */
@Module
@InstallIn(SingletonComponent::class)
object ModuloSimulador {

    @Provides
    @Singleton
    fun proveerPrecios(): PreciosAceiteRepositorio = PreciosAceiteRepositorioImpl()

    @Provides
    @Singleton
    fun proveerPreferencias(@ApplicationContext contexto: Context): PreferenciasSimulador =
        PreferenciasSimuladorDataStore(contexto)
}
