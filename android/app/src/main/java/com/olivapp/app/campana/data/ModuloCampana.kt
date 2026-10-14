package com.olivapp.app.campana.data

import com.olivapp.app.campana.domain.CampanasRepositorio
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Repositorio de campañas (ADR 0008: DI con Hilt). */
@Module
@InstallIn(SingletonComponent::class)
object ModuloCampana {

    @Provides
    @Singleton
    fun proveerCampanas(): CampanasRepositorio = CampanasRepositorioImpl()
}
