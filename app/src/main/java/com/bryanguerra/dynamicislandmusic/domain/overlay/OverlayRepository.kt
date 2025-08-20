package com.bryanguerra.dynamicislandmusic.domain.overlay

import kotlinx.coroutines.flow.StateFlow

interface OverlayRepository {
    val isPillVisible: StateFlow<Boolean>
    fun showPill()
    fun hidePill()
}