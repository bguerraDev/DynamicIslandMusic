// MediaNotificationListener.kt
package com.bryanguerra.dynamicislandmusic.services

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.bryanguerra.dynamicislandmusic.util.Constants
import com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus
import kotlinx.coroutines.*

class MediaNotificationListener : NotificationListenerService() {

    private lateinit var msm: MediaSessionManager
    private var activeController: MediaController? = null

    // scope propio para timers
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var inactivityJob: Job? = null
    private var lostSessionJob: Job? = null
    private var lastPauseAtMs: Long = 0L
    private var lastKnownState: Int? = null

    // ventanas de gracia
    private val pausedAutoHideMs = 15_000L      // TODO modificable según preferencia. MEJORAR
    private val transientNoneGraceMs = 1_000L   // ignora NONE/STOPPED durante 1s tras PAUSED
    private val lostSessionGraceMs = 1_500L     // espera 1.5s si “pierdes” la sesión activa

    override fun onListenerConnected() {
        super.onListenerConnected()
        msm = getSystemService(MediaSessionManager::class.java)

        val self = ComponentName(this, javaClass)

        // IMPORTANTE: registrar con ComponentName
        msm.addOnActiveSessionsChangedListener(
            { controllers -> onActiveSessionsChanged(controllers ?: emptyList()) },
            self
        )

        // Y pedir sesiones con el ComponentName (NO null)
        val initial = safeGetActiveSessions(self)
        onActiveSessionsChanged(initial)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // No necesitamos nada aquí
    }

    override fun onDestroy() {
        activeController?.unregisterCallback(cb)
        cancelInactivityTimer()
        lostSessionJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun safeGetActiveSessions(self: ComponentName): List<MediaController> =
        try {
            msm.getActiveSessions(self) ?: emptyList()
        } catch (se: SecurityException) {
            // Todavía no se tiene concedido el acceso de notificaciones.
            emptyList()
        }

    private fun onActiveSessionsChanged(controllers: List<MediaController>) {
        // Filtra RiMusic
        val candidate = controllers.firstOrNull { it.packageName == Constants.TARGET_PLAYER_PKG }

        if (candidate?.sessionToken != activeController?.sessionToken) {
            // Si perdemos la sesión, NO ocultar de golpe: programa ocultado diferido
            if (candidate == null && activeController != null) {
                lostSessionJob?.cancel()
                lostSessionJob = serviceScope.launch {
                    delay(lostSessionGraceMs)
                    // Solo ocultar si seguimos sin sesión tras la gracia
                    if (activeController == null) {
                        cancelInactivityTimer()
                        hideIsland()
                    }
                }
            }

            activeController?.unregisterCallback(cb)
            activeController = candidate
            MediaSessionBus.attachController(activeController)
            activeController?.registerCallback(cb)
        }

        // Si sí hay sesión tras el cambio, cancela el ocultado diferido
        if (activeController != null) {
            lostSessionJob?.cancel()
            lostSessionJob = null
        }

        // Manejo del estado inicial (incluye PAUSED)
        val state = activeController?.playbackState?.state
        when (state) {

            PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING -> {
                cancelInactivityTimer()
                showIsland()
            }

            PlaybackState.STATE_PAUSED -> {
                lastPauseAtMs = System.currentTimeMillis()
                showIsland()
                restartInactivityTimer()
            }

            PlaybackState.STATE_STOPPED, PlaybackState.STATE_NONE, null -> {
                // Aplica debounce: si venimos de PAUSED y esto es muy inmediato, ignóralo
                val now = System.currentTimeMillis()
                val recentPause = now - lastPauseAtMs <= transientNoneGraceMs
                if (!recentPause) {
                    cancelInactivityTimer()
                    hideIsland()
                }
            }

            else -> Unit
        }
        lastKnownState = state
    }

    private val cb = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val s = state?.state
            MediaSessionBus.updatePlaybackState(s)
            MediaSessionBus.updatePlayback(state)

            when (s) {

                PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING -> {
                    cancelInactivityTimer()
                    showIsland()
                }

                PlaybackState.STATE_PAUSED -> {
                    lastPauseAtMs = System.currentTimeMillis()
                    showIsland() // mantener visible
                    restartInactivityTimer() // autohide opcional
                }

                PlaybackState.STATE_STOPPED, PlaybackState.STATE_NONE, null -> {
                    // Debounce: ignora NONE/STOPPED si llegan justo tras PAUSED (panel de notificaciones)
                    val now = System.currentTimeMillis()
                    val recentPause = now - lastPauseAtMs <= transientNoneGraceMs
                    if (recentPause) {
                        // No hacemos nada; dejamos que el timer de pausa lo maneje
                        return
                    }
                    cancelInactivityTimer()
                    hideIsland()
                }

                else -> Unit
            }
            lastKnownState = s
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            MediaSessionBus.updateMetadata(metadata)
        }
    }

    /**
     * Helpers de visibilidad / timers
     */

    private fun showIsland() {
        // Idempotente: IslandForegroundService.start debería manejar llamadas repetidas
        IslandForegroundService.start(this)
    }

    private fun hideIsland() {
        IslandForegroundService.stop(this)
    }

    private fun restartInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = serviceScope.launch {
            delay(pausedAutoHideMs)
            // Si siguiera en pausa tras el delay, ocultamos
            val currentState = activeController?.playbackState?.state
            if (currentState == PlaybackState.STATE_PAUSED) {
                hideIsland()
            }
        }
    }

    private fun cancelInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = null
    }

}