package com.olivapp.app.acceso.data

import android.content.Context
import com.olivapp.app.acceso.domain.CuentasRepositorio
import com.olivapp.app.acceso.domain.SesionRepositorio
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Repositorios de acceso y sesión (ADR 0008: DI con Hilt). */
@Module
@InstallIn(SingletonComponent::class)
object ModuloAcceso {

    @Provides
    @Singleton
    fun proveerSesion(@ApplicationContext contexto: Context): SesionRepositorio =
        AlmacenSesiones(contexto)

    @Provides
    @Singleton
    fun proveerCuentas(): CuentasRepositorio = CuentasRepositorioImpl()
}
