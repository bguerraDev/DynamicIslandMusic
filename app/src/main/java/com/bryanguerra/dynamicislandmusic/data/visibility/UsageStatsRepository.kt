package com.bryanguerra.dynamicislandmusic.data.visibility

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.core.content.getSystemService

class UsageStatsRepository(private val context: Context) {

    // KTX devuelve T?; en API 29+ debería existir, pero lo tratamos con cuidado.
    private val usageStatsManager: UsageStatsManager? = context.getSystemService()

    /**
     * Comprueba si [packageName] está en foreground mirando eventos recientes.
     * Prioriza MOVE_TO_FOREGROUND/BACKGROUND y, si no hay, usa ACTIVITY_* como fallback.
     */
    fun isAppInForeground(packageName: String, timeWindowMillis: Long = 10_000L): Boolean {
        val mgr = usageStatsManager ?: return false

        val end = System.currentTimeMillis()
        val start = end - timeWindowMillis
        val events: UsageEvents = mgr.queryEvents(start, end)
        val e = UsageEvents.Event()

        var inFg = false
        var sawMoveEvent = false

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.packageName != packageName) continue

            when (e.eventType) {
                // App-level (preferidos)
                UsageEvents.Event.MOVE_TO_FOREGROUND -> { inFg = true;  sawMoveEvent = true }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> { inFg = false; sawMoveEvent = true }

                // Fallback activity-level (si no vimos MOVE_*)
                UsageEvents.Event.ACTIVITY_RESUMED -> if (!sawMoveEvent) inFg = true
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> if (!sawMoveEvent) inFg = false
            }
        }
        return inFg
    }
}