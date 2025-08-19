package com.bryanguerra.dynamicislandmusic.ui.animations

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/** Centraliza tiempos y curvas para que to do se mueva igual. */
object AnimSpecs {
    // Duraciones más suaves/lentas que antes (estilo iOS)
    const val GenieOpenMs = 520
    const val GenieCloseMs = 440
    const val DimInMs = 260
    const val DimOutMs = 220

    // Curvas.
    // easeOut (expulsado desde la cámara)
    val easeOut: Easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
    // easeIn (succionado hacia la cámara)
    val easeIn: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    val genieOpen = tween<Float>(durationMillis = GenieOpenMs, easing = easeOut)
    val genieClose = tween<Float>(durationMillis = GenieCloseMs, easing = easeIn)

    val dimIn = tween<Float>(durationMillis = DimInMs)
    val dimOut = tween<Float>(durationMillis = DimOutMs)
}