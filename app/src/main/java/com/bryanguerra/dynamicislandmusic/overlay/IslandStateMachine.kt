package com.bryanguerra.dynamicislandmusic.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface IslandState { data object Hidden: IslandState; data object Pill: IslandState; data object Expanded: IslandState }

class IslandStateMachine {
    private val _state = MutableStateFlow<IslandState>(IslandState.Hidden)
    val state: StateFlow<IslandState> = _state
    fun showPill() { _state.value = IslandState.Pill }
    fun expand() { _state.value = IslandState.Expanded }
    fun hide() { _state.value = IslandState.Hidden }
}