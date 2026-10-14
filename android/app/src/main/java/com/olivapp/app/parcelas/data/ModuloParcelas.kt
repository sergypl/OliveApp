package com.olivapp.app.parcelas.data

import com.olivapp.app.parcelas.domain.ParcelasRepositorio
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Repositorio de parcelas (ADR 0008: DI con Hilt). */
@Module
@InstallIn(SingletonComponent::class)
object ModuloParcelas {

    @Provides
    @Singleton
    fun proveerParcelas(): ParcelasRepositorio = ParcelasRepositorioImpl()
}
