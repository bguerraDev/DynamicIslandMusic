package com.bryanguerra.dynamicislandmusic.ui.effects

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * SKELETON CLASS FILE FOR FROSTEDGLASS EFFECT. NOT USED IN THE PROJECT.
 */

/**
 * Capa oscurecida para mostrar detrás del popup (simula “enfoque” en el popup).
 */
@Composable
fun BackdropDim(
    modifier: Modifier = Modifier,
    alpha: Float = 0.18f
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
    )
}

/**
 * Contenedor con efecto “frosted glass” (no blurea lo de atrás a nivel sistema,
 * pero crea un acabado translúcido suave sobre el propio contenido del popup).
 *
 * Úsalo envolviendo el contenido del popup.
 */
@Composable
fun FrostedGlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    tint: Color = Color.White.copy(alpha = 0.06f),
    blurRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint)
            .blur(blurRadius) // blurea el contenido del propio box para un efecto de vidrio
    ) {
        content()
    }
}

/*
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BackdropSimulatedGlass(
    alpha: Float, // 0..1 del scrim
    onTap: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onTap() } }
    ) {
        // Scrim
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)))

        // Tinte + leve desaturación (look “glass”)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val m = floatArrayOf(
                        0.6f, 0.2f, 0.2f, 0f, 0f,
                        0.2f, 0.6f, 0.2f, 0f, 0f,
                        0.2f, 0.2f, 0.6f, 0f, 0f,
                        0f,   0f,   0f,   1f, 0f
                    )
                    renderEffect = android.graphics.RenderEffect
                        .createColorFilterEffect(
                            android.graphics.ColorMatrixColorFilter(
                                android.graphics.ColorMatrix(m)
                            )
                        )
                        .asComposeRenderEffect()
                }
                .background(Color.White.copy(alpha = 0.06f))
        )

        // (Opcional) capa de “ruido” ultra sutil para textura glass
    }
}
*/
