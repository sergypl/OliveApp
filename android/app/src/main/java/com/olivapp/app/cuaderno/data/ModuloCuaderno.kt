package com.olivapp.app.cuaderno.data

import com.olivapp.app.cuaderno.domain.LaboresRepositorio
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Repositorio del cuaderno de campo (ADR 0008: DI con Hilt). */
@Module
@InstallIn(SingletonComponent::class)
object ModuloCuaderno {

    @Provides
    @Singleton
    fun proveerLabores(): LaboresRepositorio = LaboresRepositorioImpl()
}
