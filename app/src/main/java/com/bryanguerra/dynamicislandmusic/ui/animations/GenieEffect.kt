package com.bryanguerra.dynamicislandmusic.ui.animations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Progreso [0..1] de la transición tipo "genie" (estilo Dynamic Island iOS).
 * 0f = colapsada (pegada a la cámara), 1f = expandida.
 */
@Composable
fun rememberGenieProgress(expanded: Boolean): Float {
    val target = if (expanded) 1f else 0f
    val spec = if (expanded) AnimSpecs.genieOpen else AnimSpecs.genieClose
    val progress by animateFloatAsState(targetValue = target, animationSpec = spec, label = "genie")
    return progress
}

/**
 * Alpha del dim de fondo en función de si está expandido o no.
 */
@Composable
fun rememberDimAlpha(expanded: Boolean, maxAlpha: Float = 0.45f): Float {
    val target = if (expanded) maxAlpha else 0f
    val spec = if (expanded) AnimSpecs.dimIn else AnimSpecs.dimOut
    val alpha by animateFloatAsState(targetValue = target, animationSpec = spec, label = "dim")
    return alpha
}

/**
 * Aplica el "genie transform":
 *  - origin en top-center (cámara)
 *  - ligera traslación Y (expulsado/succionado)
 *  - scaleX/scaleY para dar sensación de curva
 *  - radio de esquina animado
 */
fun Modifier.genieTransform(
    progress: Float,
    closedCorner: Dp = 22.dp,
    openCorner: Dp = 16.dp,
    expelledYOffsetCollapsed: Dp = (-10).dp, // punto de partida cerca de la cámara
): Modifier = composed {
    val density = LocalDensity.current

    // Interpolaciones simples (lerp)
    fun lerp(start: Float, end: Float, t: Float) = start + (end - start) * t

    val scaleX = lerp(0.92f, 1f, progress)
    val scaleY = lerp(0.86f, 1f, progress)

    val translationY = with(density) {
        lerp(expelledYOffsetCollapsed.toPx(), 0f, progress)
    }

    val corner = lerp(closedCorner.value, openCorner.value, progress).dp

    this.graphicsLayer {
        // Ancla en la cámara (arriba-centro)
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.translationY = translationY
        shape = RoundedCornerShape(corner)
        clip = true
    }
}