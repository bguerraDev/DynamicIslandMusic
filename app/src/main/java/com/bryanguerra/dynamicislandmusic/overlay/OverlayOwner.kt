package com.bryanguerra.dynamicislandmusic.overlay

import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Owner mínimo para que un ComposeView en WindowManager tenga Lifecycle/SavedState/ViewModelStore.
 * Controla su ciclo con onStart/onResume/onPause/onStop/onDestroy desde OverlayWindowManager.
 */
class OverlayOwner() : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val vmStore = ViewModelStore()

    init {
        // Estado inicial CREATED + SavedState disponible
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = vmStore

    fun onStart()  { lifecycleRegistry.currentState = Lifecycle.State.STARTED }
    // Alternativa “más canónica”: lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    fun onResume() { lifecycleRegistry.currentState = Lifecycle.State.RESUMED }
    // Alternativa: handleLifecycleEvent(ON_RESUME)

    // Volvemos a STARTED (de RESUMED -> STARTED)
    fun onPause() { lifecycleRegistry.currentState = Lifecycle.State.STARTED }
    // Alternativa: handleLifecycleEvent(ON_PAUSE)

    // Volvemos a CREATED (de STARTED -> CREATED)
    fun onStop() { lifecycleRegistry.currentState = Lifecycle.State.CREATED }
    // Alternativa: handleLifecycleEvent(ON_STOP)

    /** Destruye por completo (libera ViewModels también). */
    fun onDestroy() {
        // Alternativa: handleLifecycleEvent(ON_DESTROY)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        vmStore.clear()
    }
}
