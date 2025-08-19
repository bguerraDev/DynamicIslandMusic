package com.bryanguerra.dynamicislandmusic.ui.animations

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Gestos estándar de la isla con hápticos integrados.
 *  - Tap corto
 *  - Long press
 *  - Swipe up (para cerrar)
 */
fun Modifier.islandGestures(
    enabled: Boolean = true,
    onShortTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeUp: () -> Unit,
): Modifier = composed {
    if (!enabled) return@composed this

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { 56.dp.toPx() }

    var totalDrag by remember { mutableStateOf(0f) }

    this
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onShortTap()
                },
                onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            )
        }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    totalDrag += dragAmount
                    change.consume()
                },
                onDragEnd = {
                    if (totalDrag <= -dragThresholdPx) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSwipeUp()
                    }
                    totalDrag = 0f
                },
                onDragCancel = { totalDrag = 0f }
            )
        }
}