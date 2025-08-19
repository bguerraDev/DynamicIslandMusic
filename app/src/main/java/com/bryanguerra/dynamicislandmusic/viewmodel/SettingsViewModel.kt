package com.bryanguerra.dynamicislandmusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bryanguerra.dynamicislandmusic.data.settings.SettingsRepository
import com.bryanguerra.dynamicislandmusic.util.PermissionsHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val islandEnabled: Boolean = true,
    val overlayGranted: Boolean = false,
    val notifListenerGranted: Boolean = false,
    val usageAccessGranted: Boolean = false,
    val postNotificationsGranted: Boolean = true
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
        _ui.update {
            it.copy(
                overlayGranted = PermissionsHelper.hasOverlayPermission(ctx),
                notifListenerGranted = PermissionsHelper.hasNotificationListener(ctx),
                usageAccessGranted = PermissionsHelper.hasUsageAccess(ctx),
                postNotificationsGranted = PermissionsHelper.hasPostNotifications(ctx)
            )
        }
    }

    fun setIslandEnabled(value: Boolean) {
        viewModelScope.launch { repo.setIslandEnabled(value) }
    }
}