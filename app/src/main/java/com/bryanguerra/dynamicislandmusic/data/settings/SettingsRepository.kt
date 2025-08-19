package com.bryanguerra.dynamicislandmusic.data.settings

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.bryanguerra.dynamicislandmusic.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val ISLAND_ENABLED = booleanPreferencesKey("island_enabled")
    }

    val islandEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ISLAND_ENABLED] ?: true } // por defecto: activo

    suspend fun setIslandEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ISLAND_ENABLED] = enabled }
    }

    fun defaultIslandPreset(): Constants.IslandSizePreset {
        val model = Build.MODEL.lowercase()
        return if (model.contains("pixel 8 pro")) Constants.PRESET_PIXEL_8_PRO
        else Constants.PRESET_PIXEL_8_PRO // fallback razonable; luego podrás añadir más
    }
}