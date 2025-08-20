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

class MediaNotificationListener : NotificationListenerService() {

    private lateinit var msm: MediaSessionManager
    private var activeController: MediaController? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        msm = getSystemService(MediaSessionManager::class.java)
        val self = ComponentName(this, javaClass)

        msm.addOnActiveSessionsChangedListener(
            { controllers -> onActiveSessionsChanged(controllers ?: emptyList()) },
            self
        )

        // Pedir sesiones con el ComponentName (NO null)
        onActiveSessionsChanged(safeGetActiveSessions(self))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // No necesitamos nada aquí
    }

    override fun onDestroy() {
        activeController?.unregisterCallback(cb)
        super.onDestroy()
    }

    private fun safeGetActiveSessions(self: ComponentName): List<MediaController> =
        try {
            msm.getActiveSessions(self) ?: emptyList()
        } catch (_: SecurityException) {
            // Todavía no se tiene concedido el acceso de notificaciones.
            emptyList()
        }

    private fun onActiveSessionsChanged(controllers: List<MediaController>) {
        // Filtra RiMusic
        val candidate = controllers.firstOrNull { it.packageName == Constants.TARGET_PLAYER_PKG }

        if (candidate?.sessionToken != activeController?.sessionToken) {
            // Si perdemos la sesión, NO ocultar de golpe: programa ocultado diferido TODO VERIFICAR QUE FUNCIONA ESTE IF DE DENTRO  if (candidate == null && activeController != null) {
            //if (candidate == null && activeController != null) {
            activeController?.unregisterCallback(cb)
            activeController = candidate
            MediaSessionBus.attachController(activeController)
            activeController?.registerCallback(cb)

            // Arrancar el FGS cuando tenemos sesión (dejar que el servicio decida si mostrar u ocultar)
            if (activeController != null) IslandForegroundService.start(this)
            //}

            // Propaga estado inicial al bus
            MediaSessionBus.updatePlaybackState(activeController?.playbackState?.state)
            MediaSessionBus.updateMetadata(activeController?.metadata)
        }
    }

    private val cb = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            MediaSessionBus.updatePlaybackState(state?.state)
            MediaSessionBus.updatePlayback(state)

            // Opcional: si se recibe eventos y el service no está, arráncarlo
            IslandForegroundService.start(this@MediaNotificationListener)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            MediaSessionBus.updateMetadata(metadata)
        }
    }
}