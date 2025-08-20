package com.bryanguerra.dynamicislandmusic.domain.overlay

import javax.inject.Inject

class HideIslandUseCase @Inject constructor(
    private val overlay: OverlayRepository
) {
    operator fun invoke() {
        overlay.hidePill()
    }
}
