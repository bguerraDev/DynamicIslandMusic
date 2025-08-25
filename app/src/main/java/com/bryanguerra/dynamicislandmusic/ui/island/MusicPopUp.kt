package com.bryanguerra.dynamicislandmusic.ui.island

import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bryanguerra.dynamicislandmusic.R
import com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus
import com.bryanguerra.dynamicislandmusic.overlay.ColorExtractor
import com.bryanguerra.dynamicislandmusic.overlay.DominantColors
import com.bryanguerra.dynamicislandmusic.ui.components.SoundWaveLottie
import kotlinx.coroutines.delay
import android.view.HapticFeedbackConstants
import androidx.compose.ui.input.pointer.pointerInput
import com.bryanguerra.dynamicislandmusic.domain.settings.WaveStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicPopUp(
    onSwipeUpClose: () -> Unit,
    selectedWave: WaveStyle) {
    // --- Estado de media ---
    val playbackStateInt by MediaSessionBus.playbackState.collectAsState(initial = null)
    val playback by MediaSessionBus.playback.collectAsState(initial = null)
    val metadata by MediaSessionBus.metadata.collectAsState(initial = null)
    val albumArt by MediaSessionBus.albumArt.collectAsState(initial = null)

    // --- Paleta dinámica ---
    var dom by remember {
        mutableStateOf(DominantColors(Color(0xFF1E1E1E), Color(0xFFF5F5F5), Color(0xFF888888)))
    }
    LaunchedEffect(albumArt) { dom = ColorExtractor.extract(albumArt) }

    val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Título"
    val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Artista"
    val durationMs = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    val isPlaying = playbackStateInt == PlaybackState.STATE_PLAYING ||
            playbackStateInt == PlaybackState.STATE_BUFFERING

    // --- Cálculo de progreso solo visual ---
    fun calcCurrentPosMs(): Long {
        val pb = playback ?: return 0L
        var base = pb.position
        if (pb.state == PlaybackState.STATE_PLAYING || pb.state == PlaybackState.STATE_BUFFERING) {
            val delta = SystemClock.elapsedRealtime() - pb.lastPositionUpdateTime
            base += (delta * pb.playbackSpeed).toLong()
        }
        return if (durationMs > 0) base.coerceIn(0L, durationMs) else base
    }
    var progress by remember(durationMs) { mutableStateOf(0f) }
    var displayedPosMs by remember { mutableStateOf(0L) }
    LaunchedEffect(durationMs, playback, isPlaying) {
        while (true) {
            val pos = calcCurrentPosMs()
            displayedPosMs = pos
            progress = if (durationMs > 0) (pos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            delay(500)
        }
    }

    // --- Bounce sutil ---
    val scale = remember { Animatable(0.92f) }
    LaunchedEffect(Unit) {
        scale.snapTo(0.92f)
        scale.animateTo(1.02f, animationSpec = tween(550))
        scale.animateTo(1.00f, animationSpec = tween(550))
    }

    val shape = RoundedCornerShape(28.dp)
    val view = LocalView.current

    // Container SIN statusBarsPadding, lo maneja el padre (ExpandedBackdrop)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            // Tap en to do el PopUp -> abrir app (onContentTap)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, drag ->
                    if (drag < -20f) onSwipeUpClose()
                }
            }
    ) {
        // 1) Fondo: carátula desenfocada (no afecta al contenido de arriba)
        AsyncImage(
            model = albumArt ?: R.drawable.ic_album_placeholder,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .blur(
                    radius = 10.dp, // 24
                    edgeTreatment = BlurredEdgeTreatment.Unbounded // sin borde duro
                )
                .background(Color.Transparent)
                .graphicsLayer { alpha = 0.90f } // 0.85 un poco visible
        )

        // 2) Capa “vidrio”: degradado translúcido + sutil borde interior
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        /*0f to dom.bg.copy(alpha = 0.75f),
                        1f to dom.bg.copy(alpha = 0.60f)*/
                        0f to dom.bg.copy(alpha = 0.42f),
                        1f to dom.bg.copy(alpha = 0.28f)
                    )
                )
        )

        // 3) Contenido nítido encima
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // -------- Fila 1 --------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = albumArt ?: R.drawable.ic_album_placeholder,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        color = dom.onBg,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Text(
                        text = artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = dom.onBg.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.width(12.dp))

                SoundWaveLottie(
                    isPlaying = isPlaying,
                    color = dom.accent,
                    modifier = Modifier.size(54.dp), // antes 26.dp
                    resId = selectedWave.rawRes
                )
            }

            Spacer(Modifier.height(12.dp))

            // -------- Fila 2 --------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatTime(displayedPosMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = dom.onBg.copy(alpha = 0.9f)
                )
                Spacer(Modifier.width(10.dp))
                StaticProgressBar(
                    progress = progress,
                    modifier = Modifier
                        .height(6.dp)
                        .weight(1f),
                    progressColor = dom.onBg.copy(alpha = 0.65f),
                    trackColor = dom.onBg.copy(alpha = 0.22f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    formatTime(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = dom.onBg.copy(alpha = 0.9f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // -------- Fila 3: Controles --------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    MediaSessionBus.previous()
                }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior", tint = dom.onBg)
                }
                IconButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    MediaSessionBus.togglePlayPause()
                }) {
                    if (isPlaying) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pausar", tint = dom.onBg)
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir", tint = dom.onBg)
                    }
                }
                IconButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    MediaSessionBus.next()
                }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente", tint = dom.onBg)
                }
            }
        }
    }
}

@Composable
private fun StaticProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color
) {
    Box(
        modifier = modifier.background(trackColor, RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(progressColor, RoundedCornerShape(999.dp))
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}