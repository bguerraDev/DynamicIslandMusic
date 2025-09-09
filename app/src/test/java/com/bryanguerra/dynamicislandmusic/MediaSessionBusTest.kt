package com.bryanguerra.dynamicislandmusic

import android.media.session.PlaybackState
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest=Config.NONE)
class MediaSessionBusTest {

    @Test
    fun `updatePlaybackState with null defaults to STATE_NONE`() = runTest {
        // Arrange: Set an initial state to something other than NONE.
        val initialPlaybackState = PlaybackState.Builder()
            .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
            .build()
        MediaSessionBus.updatePlayback(initialPlaybackState)

        // Act: Call the function with a null state, simulating session loss.
        MediaSessionBus.updatePlaybackState(null)

        // Assert: The full playback object's state should default to STATE_NONE.
        val finalPlaybackState = MediaSessionBus.playback.first()
        assertEquals(PlaybackState.STATE_NONE, finalPlaybackState?.state)
    }
}
