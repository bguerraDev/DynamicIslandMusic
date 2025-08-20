package com.bryanguerra.dynamicislandmusic.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.util.TypedValue
import android.view.*
import androidx.compose.ui.platform.ComposeView
import com.bryanguerra.dynamicislandmusic.util.PermissionsHelper
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.graphics.Rect
import android.view.View
import com.bryanguerra.dynamicislandmusic.presentation.navigation.IslandNavigator
import com.bryanguerra.dynamicislandmusic.ui.island.IslandOverlay

class OverlayWindowManager(
    private val context: Context,
    private val islandNavigator: IslandNavigator

) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: ComposeView? = null
    private var owner: OverlayOwner? = null
    private var showing = false

    /**
     * helpers gestos en barra de notificaciones
     */

    private fun applySystemGestureExclusion(v: View) {
        v.post {
            val r = Rect(0, 0, v.width, v.height)
            v.systemGestureExclusionRects = listOf(r)
        }
    }

    /**
     * helpers dp -> px
     */
    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()

    private fun pillLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, // 👈 solo lo que mide el pill
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL // toques fuera pasan a la app
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL // 👈 centrado arriba
            y = dpToPx(8) // 👈 pegado arriba pero sin solapar status bar
            @Suppress("DEPRECATION")
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

    fun showIsland() {
        if (showing) { Log.d("OverlayWindowManager", "showIsland(): already showing"); return }

        if (!PermissionsHelper.hasOverlayPermission(context)) { Log.d("OverlayWindowManager", "showIsland(): no overlay permission"); return }

        Log.d("OverlayWindowManager", "showIsland(): adding view")
        // Construir la ComposeView y owners de forma segura
        // Necesario para que Compose encuentre lifecycle/savedState/vmStore
        // Use the extension functions from the ViewTree* classes on the root view directly
        val view: ComposeView = root ?: ComposeView(context).also { compose ->
            val overlayOwner = OverlayOwner()
            compose.setViewTreeLifecycleOwner(overlayOwner)
            compose.setViewTreeSavedStateRegistryOwner(overlayOwner)
            compose.setViewTreeViewModelStoreOwner(overlayOwner)
            owner = overlayOwner

            // setContent después de settear los owners
            // Importante: el composable puede pedir expandir/colapsar para que actualicemos la ventana
            compose.setContent {
                IslandOverlay(
                    onShortTap = {
                        // ocultar pill mientras está expandido
                        hide()
                        islandNavigator.openExpanded()
                    },
                    onLongPress = {
                        hide()
                        // TODO abrir app de destino (RiMusic)
                        //openTargetApp(Constants.TARGET_PLAYER_PKG)
                    }
                )
            }
            root = compose
        }

        val lp = pillLayoutParams()
        try {
            wm.addView(view, lp)            // usa 'view' local, no 'root!!'
            owner?.onStart()
            owner?.onResume()
            showing = true
            applySystemGestureExclusion(view)
        } catch (e: SecurityException) {
            Log.w("Overlay", "Cannot add overlay (perm/OEM): ${e.message}")
            cleanup()
        } catch (e: WindowManager.BadTokenException) {
            Log.w("Overlay", "Cannot add overlay (bad token): ${e.message}")
            cleanup()
        } catch (e: IllegalStateException) {
            Log.w("Overlay", "Cannot add overlay (state): ${e.message}")
            cleanup()
        }
    }

    private fun cleanup() {
        // deja to-do en estado consistente para el siguiente intento
        owner?.onDestroy()
        owner = null
        root = null
        showing = false
    }

    fun hide() {
        if (!showing) { Log.d("OverlayWindowManager", "hide(): already hidden"); return }
        Log.d("OverlayWindowManager", "hide(): removing view")
        // 1) Retrocede lifecycle
        // Call lifecycle methods before view removal if appropiate
        runCatching { owner?.onPause(); owner?.onStop() }
        // 2) Quita la vista del WindowManager
        runCatching { root?.let { wm.removeViewImmediate(it) } }
        // 3) Destruye owner y libera referencias (VMs)
        runCatching { owner?.onDestroy() }
        owner = null // Release reference to avoid leaks OverlayOwner
        root = null // Release reference to avoid leaks ComposeView
        showing = false // Reset state
    }
}
