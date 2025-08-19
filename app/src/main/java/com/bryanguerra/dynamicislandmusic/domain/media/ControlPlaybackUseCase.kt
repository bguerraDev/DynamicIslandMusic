package com.bryanguerra.dynamicislandmusic.domain.media

import com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus
import javax.inject.Inject

class ControlPlaybackUseCase @Inject constructor() {
    fun previous() = MediaSessionBus.previous()
    fun next() = MediaSessionBus.next()
    fun toggle() = MediaSessionBus.togglePlayPause()
}