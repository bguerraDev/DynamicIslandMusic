package com.bryanguerra.dynamicislandmusic.presentation.navigation

import android.content.Context
import android.content.Intent
import com.bryanguerra.dynamicislandmusic.IslandExpandedActivity

class IslandNavigatorImplementation(
    private val appContext: Context
) : IslandNavigator {

    override fun openExpanded() {
        val intent = Intent(appContext, IslandExpandedActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        appContext.startActivity(intent)
    }

    override fun closeExpanded() {
        // El propio activity hace finish(); aquí no hacemos nada extra
    }
}