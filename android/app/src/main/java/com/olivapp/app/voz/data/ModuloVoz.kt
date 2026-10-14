package com.olivapp.app.voz.data

import android.content.Context
import com.olivapp.app.voz.domain.GrabadorDeNotas
import com.olivapp.app.voz.domain.NotasRepositorio
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Repositorio de notas de voz (ADR 0008: DI con Hilt). */
@Module
@InstallIn(SingletonComponent::class)
object ModuloVoz {

    @Provides
    @Singleton
    fun proveerNotas(@ApplicationContext contexto: Context): NotasRepositorio =
        NotasRepositorioImpl(AlmacenNotasVoz(contexto))

    /**
     * Una grabadora nueva en cada punto de inyección (cada pantalla graba por
     * su cuenta), igual que cuando cada composable creaba la suya.
     */
    @Provides
    fun proveerGrabador(): GrabadorDeNotas = GrabadorNotas()
}
