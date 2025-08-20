package com.bryanguerra.dynamicislandmusic.overlay

import android.media.session.PlaybackState
import android.util.Log
import com.bryanguerra.dynamicislandmusic.domain.overlay.HideIslandUseCase
import com.bryanguerra.dynamicislandmusic.domain.overlay.ShowIslandUseCase
import com.bryanguerra.dynamicislandmusic.util.Constants
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface IslandState {
    data object Hidden : IslandState
    data object Pill : IslandState
    data object Expanded : IslandState
}

/**
 * Reglas y efectos (show/hide) se concentran aquí.
 * - No conoce Android UI, solo llama a use cases.
 * - Lleva timers de pausa y debounce tras PAUSED.
 */
@ServiceScoped
class IslandStateMachine @Inject constructor(
    private val showIsland: ShowIslandUseCase,
    private val hideIsland: HideIslandUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<IslandState>(IslandState.Hidden)
    val state: StateFlow<IslandState> = _state

    // Entorno (reglas externas)
    private data class Env(
        val enabled: Boolean = true,
        val unlocked: Boolean = true,
        val targetInForeground: Boolean = false
    )

    private var env = Env()

    // Playback
    private var lastPlayback: Int? = null
    private var lastPauseAtMs: Long = 0L
    private var pausedTimer: Job? = null

    private val pausedAutoHideMs = Constants.INACTIVITY_TIMEOUT_MS
    private val transientNoneGraceMs = 1_000L // ignora NONE cercano a PAUSED

    /**
     * Actuar inmediatamente al cambiar el entorno
     */
    fun updateEnvironment(
        enabled: Boolean? = null,
        unlocked: Boolean? = null,
        targetInForeground: Boolean? = null
    ) {
        val prev = env
        val newEnv = prev.copy(
            enabled = enabled ?: prev.enabled,
            unlocked = unlocked ?: prev.unlocked,
            targetInForeground = targetInForeground ?: prev.targetInForeground
        )
        if (newEnv == prev) return

        Log.d(
            "IslandStateMachine",
            "env changed: enabled=${newEnv.enabled} unlocked=${newEnv.unlocked} targetInFg=${newEnv.targetInForeground}"
        )
        env = newEnv

        if (!allowed()) {
            // Desactivar -> oculta ya y cancela timers
            cancelPausedTimer()
            transitionToHidden()
            return
        }

        // Activar -> aplica YA con el último playback conocido (sin esperar otro evento)
        when (lastPlayback) {
            PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING -> {
                cancelPausedTimer()
                transitionToPill()
            }

            PlaybackState.STATE_PAUSED -> {
                transitionToPill()
                restartPausedTimer()
            }

            else -> Unit // si está STOPPED/NONE/null, no mostramos nada
        }
    }

    /**
     * Sin reentradas; evaluar directamente el estado recibido
     */
    fun onPlaybackChanged(state: Int?) {
        if (state != lastPlayback) Log.d("IslandStateMachine", "playback changed: $state")
        lastPlayback = state

        when (state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING -> {
                cancelPausedTimer()
                if (allowed()) transitionToPill() else transitionToHidden()
            }

            PlaybackState.STATE_PAUSED -> {
                lastPauseAtMs = System.currentTimeMillis()
                if (allowed()) {
                    transitionToPill()
                    restartPausedTimer()
                } else {
                    cancelPausedTimer()
                    transitionToHidden()
                }
            }

            PlaybackState.STATE_STOPPED, PlaybackState.STATE_NONE, null -> {
                val recentPause = System.currentTimeMillis() - lastPauseAtMs <= transientNoneGraceMs
                if (recentPause) {
                    Log.d("IslandStateMachine", "debounce NONE after PAUSED, ignoring")
                    return
                }
                cancelPausedTimer()
                transitionToHidden()
            }
        }
    }

    private fun allowed(): Boolean =
        env.enabled && env.unlocked && !env.targetInForeground

    private fun restartPausedTimer() {
        cancelPausedTimer()
        pausedTimer = scope.launch {
            delay(pausedAutoHideMs)
            // sigue en pausa => ocultar
            if (lastPlayback == PlaybackState.STATE_PAUSED) {
                transitionToHidden()
            }
        }
    }

    private fun cancelPausedTimer() {
        pausedTimer?.cancel()
        pausedTimer = null
    }

    private fun transitionToPill() {

        if (_state.value == IslandState.Pill) return
        Log.d("IslandStateMachine", "transition: ${_state.value} -> Pill")
        showIsland()
        _state.value = IslandState.Pill
    }

    fun transitionToExpanded() {
        _state.value = IslandState.Expanded
    }

    private fun transitionToHidden() {
        if (_state.value == IslandState.Hidden) return
        Log.d("IslandStateMachine", "transition: ${_state.value} -> Hidden")
        hideIsland()
        _state.value = IslandState.Hidden
    }
}