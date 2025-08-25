package com.bryanguerra.dynamicislandmusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.bryanguerra.dynamicislandmusic.R

@Composable
fun SoundWaveLottie(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
    @androidx.annotation.RawRes resId: Int = R.raw.soundwave // valor por defecto
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))

    // Si está en pausa, detenemos la animación (speed = 0)
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying && !isPaused,
        iterations = LottieConstants.IterateForever,
        speed = if (isPaused) 0f else 1f
    )

    // Teñido global (si tu JSON usa "Fill 1", "**" lo cubre to do)
    val dynamicColor = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = color.toArgb(),
            keyPath = arrayOf("**") // si tu compo usa "Fill 1" o "Wave", pon el nombre exacto
            //Si ves que el color no cambia, cambia el keyPath a:
            //arrayOf("**","Fill 1","**") o bien apunta por capa: arrayOf("Bar 1","**")
        )
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        dynamicProperties = dynamicColor,
        modifier = modifier
    )
}