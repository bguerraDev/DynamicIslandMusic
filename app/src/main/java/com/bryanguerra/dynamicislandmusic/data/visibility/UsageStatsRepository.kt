package com.bryanguerra.dynamicislandmusic.data.visibility

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService

private const val TAG = "UsageStatsRepository"
private const val LOG_SAMPLE_MS = 15_000L // throttling de logs
class UsageStatsRepository(private val context: Context) {

    private val usm: UsageStatsManager? = context.getSystemService()

    // Último estado conocido por paquete: inFg + timestamp del ÚLTIMO evento visto
    private val lastKnown = mutableMapOf<String, Pair<Boolean, Long>>()

    // Control de logs
    private var lastLoggedInFg: Boolean? = null
    private var lastLoggedType: Int? = null
    private var lastLogAtMs: Long = 0L

    /**
     * Regla:
     * - Si hay eventos en la ventana, el *último* evento decide (foreground/background) y se guarda.
     * - Si NO hay eventos en la ventana, devolvemos el último estado conocido durante [staleFallbackMs].
     *   Si está demasiado “viejo”, devolvemos false.
     *
     * Con esto evitamos:
     *  - Retrasos al salir (porque veremos MOVE_TO_BACKGROUND/ACTIVITY_PAUSED pronto).
     *  - Reapariciones dentro de la app (porque no forzamos false por “edad” si no hay eventos).
     */
    fun isAppInForeground(
        packageName: String,
        windowMs: Long = 30_000L,     // ventana amplia para capturar el BACKGROUND al salir
        staleFallbackMs: Long = 20_000L // cuánto tiempo respetamos el último estado si no hay eventos
    ): Boolean {
        val mgr = usm ?: return false
        val now = System.currentTimeMillis()
        val start = now - windowMs

        val events = mgr.queryEvents(start, now)
        val e = UsageEvents.Event()

        var lastType: Int? = null
        var lastTs = -1L

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.packageName != packageName) continue
            if (e.timeStamp >= lastTs) {
                lastTs = e.timeStamp
                lastType = e.eventType
            }
        }

        return if (lastType != null) {
            val inFg = when (lastType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> true

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> false

                else -> lastKnown[packageName]?.first ?: false
            }
            lastKnown[packageName] = inFg to lastTs
            logInFg(packageName, lastType, now - lastTs, inFg)
            inFg
        } else {
            val cached = lastKnown[packageName]
            val result = if (cached != null && now - cached.second <= staleFallbackMs) {
                cached.first // “pega” el último estado un rato para evitar parpadeos
            } else {
                false
            }
            logInFg(packageName, null, (cached?.second?.let { now - it } ?: -1L), result)
            result
        }
    }

    private fun logInFg(packageName: String, lastType: Int?, ageMs: Long, inFg: Boolean) {
        val now = System.currentTimeMillis()
        val changed = (inFg != lastLoggedInFg) || (lastType != lastLoggedType)
        val throttled = now - lastLogAtMs >= LOG_SAMPLE_MS
        if (changed || throttled) {
            Log.d(TAG, "[$packageName] lastType=$lastType age=${ageMs}ms -> inFg=$inFg")
            lastLoggedInFg = inFg
            lastLoggedType = lastType
            lastLogAtMs = now
        }
    }
}