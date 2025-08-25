// domain/settings/WaveStyle.kt
package com.bryanguerra.dynamicislandmusic.domain.settings

import androidx.annotation.RawRes
import com.bryanguerra.dynamicislandmusic.R

enum class WaveStyle(
    @RawRes val rawRes: Int,
    val label: String
) {
    Classic(R.raw.soundwave,  "Clásico"),
    Voice(R.raw.audio_voice,  "Voz"),
    Heartbeat(R.raw.heartbeat,"Latido");
}