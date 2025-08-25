package com.bryanguerra.dynamicislandmusic.data.settings

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bryanguerra.dynamicislandmusic.domain.settings.WaveStyle
import com.bryanguerra.dynamicislandmusic.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ISLAND_ENABLED = booleanPreferencesKey("island_enabled")
        val KEY_WAVE_STYLE = stringPreferencesKey("wave_style")
    }

    val islandEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ISLAND_ENABLED] ?: true } // por defecto: activo

    val waveStyleFlow: Flow<WaveStyle> =
        context.dataStore.data.map { prefs ->
            val name = prefs[Keys.KEY_WAVE_STYLE]
            name?.let { runCatching { WaveStyle.valueOf(it) }.getOrNull() } ?: WaveStyle.Classic

        }

    suspend fun setIslandEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ISLAND_ENABLED] = enabled }
    }

    suspend fun setWaveStyle(style: WaveStyle) {
        context.dataStore.edit { it[Keys.KEY_WAVE_STYLE] = style.name }
    }

    fun defaultIslandPreset(): Constants.IslandSizePreset {
        val model = Build.MODEL.lowercase()
        return if (model.contains("pixel 8 pro")) Constants.PRESET_PIXEL_8_PRO
        else Constants.PRESET_PIXEL_8_PRO // fallback razonable; luego podrás añadir más
    }
}