package com.olivapp.app.perfil.data

import com.olivapp.app.perfil.domain.PerfilRepositorio
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Repositorio del perfil del socio (ADR 0008: DI con Hilt). */
@Module
@InstallIn(SingletonComponent::class)
object ModuloPerfil {

    @Provides
    @Singleton
    fun proveerPerfil(): PerfilRepositorio = PerfilRepositorioImpl()
}
