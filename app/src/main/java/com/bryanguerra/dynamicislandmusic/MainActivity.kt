package com.bryanguerra.dynamicislandmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.bryanguerra.dynamicislandmusic.app.DynamicIslandTheme
import com.bryanguerra.dynamicislandmusic.ui.settings.SettingsScreen
import com.bryanguerra.dynamicislandmusic.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsVM: SettingsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynamicIslandTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SettingsScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsVM.refreshPermissions()
    }
}