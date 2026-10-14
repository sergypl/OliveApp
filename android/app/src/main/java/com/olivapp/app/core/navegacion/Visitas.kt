package com.olivapp.app.core.navegacion

import androidx.activity.ComponentActivity
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Las visitas a pantallas de esta sesión de la app: cada visita tiene su propia
 * tienda de ViewModels (la semántica de una entrada de la barra de navegación,
 * sin Navigation library).
 *
 * La tienda de la visita actual sobrevive a la rotación (vive en un ViewModel
 * de la Activity) y se cierra al abandonar la pantalla: sus ViewModels reciben
 * onCleared (limpieza real de recursos) y la siguiente visita empieza con
 * estado limpio.
 */
@HiltViewModel
class VisitasViewModel @Inject constructor() : ViewModel() {
    private val tiendas = mutableMapOf<Int, ViewModelStore>()

    /** Tienda de ViewModels de una visita (se crea la primera vez que se pide). */
    fun tiendaDe(visita: Int): ViewModelStore = tiendas.getOrPut(visita) { ViewModelStore() }

    /** Cierra la tienda de una visita cuando se abandona su pantalla. */
    fun cerrar(visita: Int) {
        tiendas.remove(visita)?.clear()
    }

    override fun onCleared() {
        // Al terminar la app (fin de la Activity) ninguna visita queda abierta.
        for (tienda in tiendas.values) {
            tienda.clear()
        }
        tiendas.clear()
    }
}

/**
 * Propietario de los ViewModels de una visita: la tienda es la de la visita y
 * la fábrica la de la Activity (Hilt). Igual que hace una entrada de la barra
 * de navegación de Navigation con sus back stack entries.
 */
class PropietarioDeVisita(
    private val tienda: ViewModelStore,
    private val actividad: ComponentActivity
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

    override val viewModelStore: ViewModelStore
        get() = tienda

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = actividad.defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() = actividad.defaultViewModelCreationExtras
}
