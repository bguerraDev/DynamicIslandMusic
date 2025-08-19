package com.bryanguerra.dynamicislandmusic.data.media

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object MediaSessionBus {
    private var controller: MediaController? = null

    private val _activePackage = MutableStateFlow<String?>(null)
    val activePackage: StateFlow<String?> = _activePackage

    // Estado númerico (por Compat)
    private val _playbackState = MutableStateFlow<Int?>(null)
    val playbackState: StateFlow<Int?> = _playbackState

    // Estado completo (para progress)
    private val _playback = MutableStateFlow<PlaybackState?>(null)
    val playback: StateFlow<PlaybackState?> = _playback

    private val _metadata = MutableStateFlow<MediaMetadata?>(null)
    val metadata: StateFlow<MediaMetadata?> = _metadata

    private val _albumArt = MutableStateFlow<Bitmap?>(null)
    val albumArt: StateFlow<Bitmap?> = _albumArt

    fun attachController(c: MediaController?) {
        controller = c
        _activePackage.value = c?.packageName
        _playbackState.value = c?.playbackState?.state
        _metadata.value = c?.metadata
        _albumArt.value = c?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: c?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
    }

    fun updatePlaybackState(state: Int?) {
        _playbackState.value = state
        // intenta mantener el objeto de PlaybackState actualizado
        val current = _playback.value
        if (current != null && current.state != state) {
            _playback.value = PlaybackState.Builder(current).setState(
                state ?: PlaybackState.STATE_PAUSED,
                current.position,
                current.playbackSpeed
            ).build()
        }
    }

    fun updatePlayback(playback: PlaybackState?) {
        _playback.value = playback
        _playbackState.value = playback?.state
    }

    fun updateMetadata(meta: MediaMetadata?) {
        _metadata.value = meta
        _albumArt.value = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
    }

    // ---- Controles reales ----
    fun play() = controller?.transportControls?.play()
    fun pause() = controller?.transportControls?.pause()
    fun togglePlayPause() {
        when (_playbackState.value) {
            PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING -> pause()
            else -> play()
        }
    }

    fun next() = controller?.transportControls?.skipToNext()
    fun previous() = controller?.transportControls?.skipToPrevious()
}