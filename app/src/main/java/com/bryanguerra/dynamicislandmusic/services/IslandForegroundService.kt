package com.bryanguerra.dynamicislandmusic.services

import android.app.*
import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bryanguerra.dynamicislandmusic.R
import com.bryanguerra.dynamicislandmusic.util.Constants
import com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus
import com.bryanguerra.dynamicislandmusic.data.visibility.UsageStatsRepository
import kotlinx.coroutines.*
import com.bryanguerra.dynamicislandmusic.data.settings.SettingsRepository
import com.bryanguerra.dynamicislandmusic.overlay.IslandState
import com.bryanguerra.dynamicislandmusic.overlay.IslandStateMachine
import com.bryanguerra.dynamicislandmusic.util.PermissionsHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val POLL_MS = 700L // sube a 700–1000ms para menos spam/CPU
@AndroidEntryPoint
class IslandForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Inject
    lateinit var stateMachine: IslandStateMachine

    @Inject
    lateinit var usageRepo: UsageStatsRepository

    @Inject
    lateinit var keyguard: KeyguardManager

    @Inject
    lateinit var settingsRepo: SettingsRepository

    @Inject
    lateinit var notificationManager: NotificationManager
    private var pollJob: Job? = null
    private var stateJob: Job? = null

    private var islandEnabled = true
    private var lastPlayback: Int? = null
    private var lastReportedInFg: Boolean? = null


    override fun onCreate() {
        super.onCreate()
        ensureChannel()

        // Arrancar FGS
        startForeground(Constants.NOTIF_ID, buildNotification())

        // 1) Observa settings (enabled)
        /*scope.launch {
            settingsRepo.islandEnabledFlow.collect { enabled ->
                Log.d("IslandForegroundService", "settings islandEnabled=$enabled")
                islandEnabled = enabled
                stateMachine.updateEnvironment(enabled = enabled)
            }
        }*/

        scope.launch {
            settingsRepo.islandEnabledFlow.collect { enabled ->
                stateMachine.updateEnvironment(enabled = enabled)
                Log.d("IslandForegroundService", "settings islandEnabled=$enabled")
                if (enabled) {
                    // “nudge” inmediato por si no llega ningún evento de playback ahora mismo
                    stateMachine.onPlaybackChanged(MediaSessionBus.playbackState.value)
                }
            }
        }

        // 2) Observa playback del bus y pásalo al stateMachine
        scope.launch {
            MediaSessionBus.playbackState.collect { state ->
                lastPlayback = state
                Log.d("IslandForegroundService", "playback=$state")
                stateMachine.onPlaybackChanged(state)
            }
        }

        // 3) Pantalla / lock
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        })

        // 4) Sondeo de foreground
        startForegroundPoll()

        // 5) Observa el estado y administra FGS/Notification
        // ✅ NO se para si hay playback aunque esté Hidden
        stateJob = scope.launch {
            stateMachine.state.collect { st ->
                val hasActivePlayback =
                    lastPlayback == android.media.session.PlaybackState.STATE_PLAYING ||
                            lastPlayback == android.media.session.PlaybackState.STATE_BUFFERING ||
                            lastPlayback == android.media.session.PlaybackState.STATE_PAUSED
                Log.d(
                    "IslandForegroundService",
                    "state=$st hasActivePlayback=$hasActivePlayback enabled=$islandEnabled"
                )
                when (st) {
                    IslandState.Pill, IslandState.Expanded -> {
                        // mantener FGS y refrescar notif
                        notificationManager.notify(Constants.NOTIF_ID, buildNotification())
                    }

                    IslandState.Hidden -> {
                        if (islandEnabled && hasActivePlayback) {
                            // seguimos vivos para vigilar cambios (salir de RiMusic / desbloquear)
                            notificationManager.notify(Constants.NOTIF_ID, buildNotification())
                        } else {
                            Log.d("IslandForegroundService", "stopping FGS")
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    }
                }
            }
        }


    }

    // --- Receiver para eventos de bloqueo/desbloqueo ---
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.d("IslandForegroundService", "screen intent=${intent?.action}")
            when (intent?.action) {
                // Usuario acaba de desbloquear: si se puede, muestra al instante
                Intent.ACTION_USER_PRESENT -> stateMachine.updateEnvironment(unlocked = true)
                // Pantalla apagada: oculta por si acaso
                Intent.ACTION_SCREEN_OFF -> stateMachine.updateEnvironment(unlocked = false)
            }
        }
    }

    private fun startForegroundPoll() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                val hasUsage = PermissionsHelper.hasUsageAccess(this@IslandForegroundService)
                val inFg = if (hasUsage) {
                    usageRepo.isAppInForeground(Constants.TARGET_PLAYER_PKG)
                } else false

                if (inFg != lastReportedInFg) {
                    Log.i("IslandForegroundService", "poll change: targetInForeground=$inFg")
                    lastReportedInFg = inFg
                    stateMachine.updateEnvironment(targetInForeground = inFg)
                }
                delay(POLL_MS) // TODO comprobar tiempo si es correcto. Antes 800ms
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        pollJob?.cancel()
        stateJob?.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---- Notificación FGS ----
    private fun ensureChannel() {
        val ch = NotificationChannel(
            Constants.NOTIF_CHANNEL_ID,
            Constants.NOTIF_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_MIN
        )
        notificationManager.createNotificationChannel(ch)
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