package com.bryanguerra.dynamicislandmusic.ui.island

import android.content.Intent
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bryanguerra.dynamicislandmusic.ui.effects.BackdropDim
import com.bryanguerra.dynamicislandmusic.ui.animations.rememberDimAlpha
import com.bryanguerra.dynamicislandmusic.util.Constants
import android.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.platform.LocalDensity
import com.bryanguerra.dynamicislandmusic.ui.effects.BackdropSimulatedGlass


@Composable
fun IslandRoot(
    onRequestClose: () -> Unit,
    onRequestExpand: () -> Unit,
    onRequestCollapse: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val dimAlpha = rememberDimAlpha(expanded, 0.45f) // alpha del “cristal” de fondo
    val context = LocalContext.current

    fun openTargetAppAndHide() {
        val pkg = Constants.TARGET_PLAYER_PKG
        context.packageManager.getLaunchIntentForPackage(pkg)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        // Succionado (cierre suave)
        expanded = false
        onRequestClose()
    }

    val localDensity = LocalDensity.current

    // === Isla (pill + contenido) ===
    if (expanded) {
        Box(Modifier.fillMaxSize()) {
            // This Box will attempt to blur what's BEHIND the window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d("IslandRoot", "Using blurred BackdropDim")
                BackdropSimulatedGlass(
                    alpha = dimAlpha,
                    onTap = { expanded = false; onRequestClose() }
                )

            } else {
                Log.d("IslandRoot", "Using fallback BackdropDim")
                // Fallback for older APIs (e.g., your BackdropDim or a simple translucent scrim)
                BackdropDim(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                expanded = false; onRequestClose()
                            }
                        },
                    alpha = 0.6f // Make it more translucent if no blur
                )
            }

            // MusicPopUp on top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp)
            ) {
                MusicPopUp(onSwipeUpClose = { expanded = false; onRequestClose() })
            }

        }
    } else {
        // ==========================
        // ISLA COLAPSADA (WRAP_CONTENT)
        // ==========================
        IslandOverlay(
            onShortTap = { expanded = true; onRequestExpand() },
            onLongPress = { /* TODO abrir app rimusic */ })

        // Garantiza que la ventana esté en WRAP_CONTENT cuando está en pill,
        // para no bloquear toques del resto de la pantalla:
        LaunchedEffect(Unit) { onRequestCollapse() }
    }

}