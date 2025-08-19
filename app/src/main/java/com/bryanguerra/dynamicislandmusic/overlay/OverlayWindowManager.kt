package com.bryanguerra.dynamicislandmusic.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.util.TypedValue
import android.view.*
import androidx.compose.ui.platform.ComposeView
import com.bryanguerra.dynamicislandmusic.ui.island.IslandRoot
import com.bryanguerra.dynamicislandmusic.util.PermissionsHelper
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.graphics.Rect
import android.os.Build
import android.view.View

class OverlayWindowManager(
    private val context: Context
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: ComposeView? = null
    private var owner: OverlayOwner? = null
    private var showing = false
    private var isExpanded = false

    fun isShowing() = showing

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

    private fun expandedLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    // ADD THIS EXPLICITLY FOR TESTING: TODO PROBAR ESTE FLAG
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    ),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            @Suppress("DEPRECATION")
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.blurBehindRadius = 99
                this.flags = this.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                this.dimAmount = 0.6f
            }*/
        }


    fun showIsland() {
        if (showing) return
        // Permiso overlay
        if (!PermissionsHelper.hasOverlayPermission(context)) {
            Log.d("OverlayWindowManager", "No tiene permiso de overlay")
            return
        }

        if (root == null) {
            root = ComposeView(context).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { // LAYER_TYPE_HARDWARE is API 11+
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    Log.d("OverlayWindowManager", "Set LAYER_TYPE_HARDWARE on ComposeView")
                }
            }
            owner = OverlayOwner().also { o ->
                // Necesario para que Compose encuentre lifecycle/savedState/vmStore
                // Use the extension functions from the ViewTree* classes on the root view directly
                root!!.setViewTreeLifecycleOwner(o)
                root!!.setViewTreeSavedStateRegistryOwner(o)
                root!!.setViewTreeViewModelStoreOwner(o)
            }
            // setContent después de settear los owners
            // Importante: el composable puede pedir expandir/colapsar para que actualicemos la ventana
            root!!.setContent {
                IslandRoot(
                    onRequestClose = { hide() },
                    onRequestExpand = { setExpanded(true) },
                    onRequestCollapse = { setExpanded(false) }
                )
            }
        }

        isExpanded = false
        val lp = pillLayoutParams()
        try {
            //root?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            wm.addView(root, lp)
            // avanza lifecycle cuando ya está en ventana
            owner?.onStart()
            owner?.onResume()
            showing = true
            applySystemGestureExclusion(root!!)
        } catch (e: SecurityException) {
            // sin permiso o política OEM — no crashear
            Log.w("Overlay", "(No permissions or OEM policy) Cannot add overlay: ${e.message}")
        } catch (e: WindowManager.BadTokenException) {
            // ventana inválida — no crashear
            Log.w("Overlay", "(Invalid window) Cannot add overlay: ${e.message}")
        } catch (e: IllegalStateException) {
            // por si acaso en algunos OEMs
            Log.w("Overlay", "(OEMs unknows) Cannot add overlay: ${e.message}")
        }
    }

    fun hide() {
        if (!showing) return

        // 1) Retrocede lifecycle
        owner?.onPause() // Call lifecycle methods before view removal if appropiate
        owner?.onStop()

        // 2) Quita la vista del WindowManager
        runCatching { wm.removeViewImmediate(root) }

        // 3) Destruye owner y libera referencias (VMs)
        owner?.onDestroy()
        owner = null // Release reference to avoid leaks OverlayOwner
        root = null // Release reference to avoid leaks ComposeView
        showing = false
    }

    private fun setExpanded(expanded: Boolean) {
        if (!showing || isExpanded == expanded) return
        isExpanded = expanded
        val lp = if (expanded) expandedLayoutParams() else pillLayoutParams()
        runCatching { wm.updateViewLayout(root, lp) }
        applySystemGestureExclusion(root!!)
    }
}
