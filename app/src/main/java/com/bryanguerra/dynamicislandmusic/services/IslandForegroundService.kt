package com.bryanguerra.dynamicislandmusic.services

import android.app.*
import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bryanguerra.dynamicislandmusic.R
import com.bryanguerra.dynamicislandmusic.util.Constants
import com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus
import com.bryanguerra.dynamicislandmusic.data.visibility.UsageStatsRepository
import com.bryanguerra.dynamicislandmusic.overlay.OverlayWindowManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import com.bryanguerra.dynamicislandmusic.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.bryanguerra.dynamicislandmusic.util.PermissionsHelper

@AndroidEntryPoint
class IslandForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Inject
    lateinit var overlay: OverlayWindowManager

    @Inject
    lateinit var usageRepo: UsageStatsRepository

    @Inject
    lateinit var keyguard: KeyguardManager

    @Inject
    lateinit var settingsRepo: SettingsRepository

    @Inject
    lateinit var notificationManager: NotificationManager
    private var inactivityJob: Job? = null
    private var pollJob: Job? = null
    private var islandEnabled: Boolean = true


    // --- Receiver para eventos de bloqueo/desbloqueo ---
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                // Usuario acaba de desbloquear: si se puede, muestra al instante
                Intent.ACTION_USER_PRESENT -> if (shouldShowOverlayNow()) showIslandIfNeeded()
                // Pantalla apagada: oculta por si acaso
                Intent.ACTION_SCREEN_OFF -> overlay.hide()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // DataStore: leer y observar el switch
        // Lectura inmediata (bloqueo muy breve) para respetar el valor desde el arranque:
        runBlocking { islandEnabled = settingsRepo.islandEnabledFlow.first() } // lectura inmediata
        // Mantenerlo actualizado en caliente:
        scope.launch {
            settingsRepo.islandEnabledFlow.collect { enabled ->
                islandEnabled = enabled
                if (!enabled) overlay.hide() // oculta al desactivar
            }
        }

        ensureChannel()
        startForeground(Constants.NOTIF_ID, buildNotification())
        // Registrar receiver dinámico
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        observeMedia()
        startConditionPoll()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        pollJob?.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        overlay.hide()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---- Observación de estado de media ----
    private fun observeMedia() {
        scope.launch {
            // Reaccionar a cambios de paquete activo + estado
            combine(
                MediaSessionBus.activePackage,
                MediaSessionBus.playbackState
            ) { pkg, state -> Pair(pkg, state) }
                .collectLatest { (pkg, state) ->
                    val isTarget = pkg == Constants.TARGET_PLAYER_PKG
                    val isPlaying = state == android.media.session.PlaybackState.STATE_PLAYING ||
                            state == android.media.session.PlaybackState.STATE_BUFFERING
                    val isPaused = state == android.media.session.PlaybackState.STATE_PAUSED

                    if (!isTarget) {
                        overlay.hide()
                        return@collectLatest
                    }
                    val canShow = shouldShowOverlayNow()

                    when {
                        isPlaying -> {
                            // Reproduciendo: no dormir
                            inactivityJob?.cancel()
                            if (canShow) showIslandIfNeeded() else overlay.hide()
                        }

                        isPaused -> {
                            // Pausado: mantener visible + dormir por timeout
                            if (canShow) showIslandIfNeeded() else overlay.hide()
                            restartPausedTimer()
                        }

                        else -> {
                            // Stopped/None/Null: dormir
                            scheduleSleep()
                        }
                    }
                }
        }
    }

    private fun showIslandIfNeeded() {

        if (!PermissionsHelper.hasOverlayPermission(this)) {
            // si revocan el permiso mientras corre, ocultar
            overlay.hide()
            return
        }
        if (!overlay.isShowing()) overlay.showIsland()
        notificationManager.notify(Constants.NOTIF_ID, buildNotification())
    }

    private fun scheduleSleep() {
        if (inactivityJob?.isActive == true) return
        inactivityJob = scope.launch {
            delay(Constants.INACTIVITY_TIMEOUT_MS)
            overlay.hide()
            // apaga el FGS si no hay overlay visible
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ---- Notificación FGS ----
    private fun ensureChannel() {
        val ch = NotificationChannel(
            Constants.NOTIF_CHANNEL_ID,
            Constants.NOTIF_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_MIN
        )
        notificationManager.createNotificationChannel(ch)
    }

    private fun startConditionPoll() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                val state = MediaSessionBus.playbackState.value
                val shouldShow = shouldShowOverlayNow()

                if (shouldShow) {
                    showIslandIfNeeded()
                } else {
                    // Oculta solo si está STOPPED/NONE o no cumple reglas (bloqueado o app en foreground)
                    val stoppedOrNone = state == android.media.session.PlaybackState.STATE_STOPPED ||
                            state == android.media.session.PlaybackState.STATE_NONE ||
                            state == null
                    val unlocked = !keyguard.isKeyguardLocked
                    val riMusicInFg = usageRepo.isAppInForeground(Constants.TARGET_PLAYER_PKG)
                    val violates = !islandEnabled || !unlocked || riMusicInFg

                    if (stoppedOrNone || violates) overlay.hide()
                    // Si es PAUSED, caerá en shouldShow=true y no entrará aquí
                }
                delay(1000)
            }
        }
    }

    private fun shouldShowOverlayNow(): Boolean {
        // reglas: playing + unlocked + RiMusic NO en foreground
        val state = MediaSessionBus.playbackState.value
        val playing = state == android.media.session.PlaybackState.STATE_PLAYING ||
                state == android.media.session.PlaybackState.STATE_BUFFERING
        val paused = state == android.media.session.PlaybackState.STATE_PAUSED


        val unlocked = !keyguard.isKeyguardLocked
        val riMusicInFg = usageRepo.isAppInForeground(Constants.TARGET_PLAYER_PKG)

        // Mostrar si: switch ON, (playing || paused), desbloqueado y RiMusic NO en foreground
        return islandEnabled && (playing || paused) && unlocked && !riMusicInFg
    }

    /**
     * Helpers de timers
     */

    private fun restartPausedTimer() {
        inactivityJob?.cancel()
        inactivityJob = scope.launch {
            delay(Constants.INACTIVITY_TIMEOUT_MS)
            overlay.hide()
            // apaga el FGS si no hay overlay visible
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, Constants.NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.fg_running))
            .setOngoing(true)
            .setSilent(true)
            .build()

    companion object {
        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, IslandForegroundService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, IslandForegroundService::class.java))
        }
    }
}