package com.bryanguerra.dynamicislandmusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bryanguerra.dynamicislandmusic.data.settings.SettingsRepository
import com.bryanguerra.dynamicislandmusic.domain.settings.WaveStyle
import com.bryanguerra.dynamicislandmusic.util.BlurSupport
import com.bryanguerra.dynamicislandmusic.util.PermissionsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val islandEnabled: Boolean = true,
    val overlayGranted: Boolean = false,
    val notifListenerGranted: Boolean = false,
    val usageAccessGranted: Boolean = false,
    val postNotificationsGranted: Boolean = true,
    val blurSupported: Boolean = false,
    val systemBlurEnabled: Boolean = false,
    val showBlurWarning: Boolean = false,
    val waveStyle: WaveStyle = WaveStyle.Classic
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    app: Application,
    private val repo: SettingsRepository
) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui

    init {
        // Observa el switch
        combine(
            repo.islandEnabledFlow,
            repo.waveStyleFlow
        ) { enabled, wave -> enabled to wave }
            .onEach { (enabled, wave) ->
                _ui.update { it.copy(islandEnabled = enabled, waveStyle = wave) }
            }
            .launchIn(viewModelScope)
        // Primera evaluación de permisos
        refreshPermissions()
    }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        val blurPossible = BlurSupport.isCrossWindowBlurPossible(ctx)
        val blurEnabled = BlurSupport.isCrossWindowBlurEnabled(ctx)
        _ui.update {
            it.copy(
                overlayGranted = PermissionsHelper.hasOverlayPermission(ctx),
                notifListenerGranted = PermissionsHelper.hasNotificationListener(ctx),
                usageAccessGranted = PermissionsHelper.hasUsageAccess(ctx),
                postNotificationsGranted = PermissionsHelper.hasPostNotifications(ctx),
                blurSupported = blurPossible, // S+ (o no)
                systemBlurEnabled = blurEnabled, // estado real del dev-option
                showBlurWarning = blurPossible && !blurEnabled
            )
        }
    }

    fun setIslandEnabled(value: Boolean) {
        viewModelScope.launch { repo.setIslandEnabled(value) }
    }

    fun setWaveStyle(style: WaveStyle) {
        viewModelScope.launch { repo.setWaveStyle(style) }
    }
}