package com.bryanguerra.dynamicislandmusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bryanguerra.dynamicislandmusic.data.settings.SettingsRepository
import com.bryanguerra.dynamicislandmusic.util.BlurSupport
import com.bryanguerra.dynamicislandmusic.util.PermissionsHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val islandEnabled: Boolean = true,
    val overlayGranted: Boolean = false,
    val notifListenerGranted: Boolean = false,
    val usageAccessGranted: Boolean = false,
    val postNotificationsGranted: Boolean = true,
    val blurSupported: Boolean = false,
    val systemBlurEnabled: Boolean = false,
    val showBlurWarning: Boolean = false
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    init {
        // Observa el switch
        viewModelScope.launch {
            repo.islandEnabledFlow.collect { enabled ->
                _ui.update { it.copy(islandEnabled = enabled) }
            }
        }
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
}