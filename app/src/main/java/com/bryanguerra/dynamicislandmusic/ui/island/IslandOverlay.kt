package com.bryanguerra.dynamicislandmusic.ui.island

import android.media.session.PlaybackState
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bryanguerra.dynamicislandmusic.R
import com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus
import com.bryanguerra.dynamicislandmusic.domain.settings.WaveStyle
import com.bryanguerra.dynamicislandmusic.overlay.ColorExtractor
import com.bryanguerra.dynamicislandmusic.overlay.DominantColors
import com.bryanguerra.dynamicislandmusic.ui.components.SoundWaveLottie

@Composable
fun IslandOverlay(
    onShortTap: () -> Unit,
    onLongPress: () -> Unit,
    cutoutWidthDp: Int = 60,   // hueco para no tapar la cámara
    leftMarginDp: Int = 2,
    rightMarginDp: Int = 2,
    selectedWave: WaveStyle
) {
    val view = LocalView.current
    val albumArt by MediaSessionBus.albumArt.collectAsState(initial = null)
    val playbackState by MediaSessionBus.playbackState.collectAsState(initial = null)

    // detectar pausa explícitamente
    val isPaused = playbackState == PlaybackState.STATE_PAUSED
    val isPlaying = playbackState == PlaybackState.STATE_PLAYING ||
            playbackState == PlaybackState.STATE_BUFFERING

    // Colores por Palette (dominante + contraste + acento)
    var dom by remember {
        mutableStateOf(
            DominantColors(
                Color(0xFF1E1E1E),
                Color(0xFFF5F5F5),
                Color(0xFF888888)
            )
        )
    }
    LaunchedEffect(albumArt) { dom = ColorExtractor.extract(albumArt) }

    val bgAnimated by animateColorAsState(
        targetValue = dom.bg.copy(alpha = 0.92f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "island-bg"
    )
    val wavesAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.3f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "waves-alpha"
    )

    Row(
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(bgAnimated.copy(alpha = 0.96f), bgAnimated.copy(alpha = 0.90f))
                )
            )
            .combinedClickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // sin ripple para look iOS
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onShortTap()
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongPress()
                }
            )
            .padding(horizontal = 12.dp, vertical = 9.dp), // padding interno sólo
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(leftMarginDp.dp))

        // Carátula mini (rectangular redondeada)
        // Carátula 20dp con radio 5dp; puedes subir a 22–24dp si te gusta más
        AsyncImage(
            model = albumArt ?: R.drawable.ic_album_placeholder,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        // Hueco central para la cámara
        // Ajusta cutoutWidthDp para más/menos hueco central (iPhone es largo; en Pixel basta con ~56–64dp).
        Spacer(Modifier.width(cutoutWidthDp.dp))

        // Soundwave Lottie tintado + pausa con isPlaying
        SoundWaveLottie(
            isPlaying = isPlaying,
            isPaused = isPaused,
            color = dom.accent,
            modifier = Modifier
                .size(36.dp)
                .alpha(wavesAlpha),
            resId = selectedWave.rawRes
        )

        Spacer(Modifier.width(rightMarginDp.dp))
    }
}

/**
 * Helper para detectar tap/long-press.
 */
suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapGesturesExt(
    onTap: () -> Unit,
    onLongPress: () -> Unit
) = detectTapGestures(onLongPress = { onLongPress() }, onTap = { onTap() })