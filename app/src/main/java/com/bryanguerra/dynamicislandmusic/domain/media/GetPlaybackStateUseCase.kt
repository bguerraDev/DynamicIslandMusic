package com.bryanguerra.dynamicislandmusic.domain.media

import com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaybackStateUseCase @Inject constructor() {
    val playbackState: Flow<Int?> = MediaSessionBus.playbackState
    val metadata = MediaSessionBus.metadata
    val albumArt = MediaSessionBus.albumArt
    val playback = MediaSessionBus.playback
}