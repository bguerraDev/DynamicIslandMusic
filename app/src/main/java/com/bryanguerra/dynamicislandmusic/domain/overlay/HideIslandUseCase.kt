package com.bryanguerra.dynamicislandmusic.domain.media.overlay

import com.bryanguerra.dynamicislandmusic.overlay.OverlayWindowManager
import javax.inject.Inject

class HideIslandUseCase @Inject constructor(
    private val overlay: OverlayWindowManager
) { operator fun invoke() = overlay.hide() }
