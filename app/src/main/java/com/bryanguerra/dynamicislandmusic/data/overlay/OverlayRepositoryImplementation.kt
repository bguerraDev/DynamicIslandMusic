package com.bryanguerra.dynamicislandmusic.data.overlay

import com.bryanguerra.dynamicislandmusic.domain.overlay.OverlayRepository
import com.bryanguerra.dynamicislandmusic.overlay.OverlayWindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class OverlayRepositoryImplementation(
    private val overlayWM: OverlayWindowManager
) : OverlayRepository {

    private val _visible = MutableStateFlow(false)
    override val isPillVisible: StateFlow<Boolean> = _visible

    override fun showPill() {
        overlayWM.showIsland()
        _visible.value = true
    }

    override fun hidePill() {
        overlayWM.hide()
        _visible.value = false
    }
}