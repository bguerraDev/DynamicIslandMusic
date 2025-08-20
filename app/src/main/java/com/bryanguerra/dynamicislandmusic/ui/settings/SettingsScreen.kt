package com.bryanguerra.dynamicislandmusic.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bryanguerra.dynamicislandmusic.util.BlurSupport
import com.bryanguerra.dynamicislandmusic.util.PermissionsHelper
import com.bryanguerra.dynamicislandmusic.viewmodel.SettingsUiState
import com.bryanguerra.dynamicislandmusic.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {

    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refreshPermissions() }

    val showBlurWarning = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !BlurSupport.isSystemBlurEnabled(ctx)
    }

    // Re-evaluar permisos al volver a la app (ej. tras abrir Ajustes)
    LaunchedEffect(Unit) {
        // pequeño "poll" porque los Settings son externos
        while (true) {
            vm.refreshPermissions()
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text("Dynamic Island (música)", style = MaterialTheme.typography.titleLarge)

        // TODO ARREGLAR ESTE WARNING DE BLUR. AÑADIRLO AL HELPER Y DEMAS PARA QUE SE ACTUALICE SI SE QUITA/PONE EL AJUSTE
        if (showBlurWarning) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Para ver el fondo desenfocado, activa \"Permitir difuminar ventanas\" en Opciones de desarrollador.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        ctx.startActivity(
                            Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) {
                        Text("Abrir opciones de desarrollador")
                    }
                }
            }
        }

        // --- Switch Activar/Desactivar ---
        Card {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Activar isla", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (ui.islandEnabled) "La isla se mostrará cuando haya música reproduciéndose"
                        else "La isla permanecerá oculta",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = ui.islandEnabled,
                    onCheckedChange = { vm.setIslandEnabled(it) }
                )
                // TODO probar para ahorrar batería
                /*val ctx = LocalContext.current

                Switch(
                    checked = ui.islandEnabled,
                    onCheckedChange = { checked ->
                        vm.setIslandEnabled(checked)
                        if (!checked) {
                            // apaga el servicio si estaba activo
                            com.bryanguerra.dynamicislandmusic.services.IslandForegroundService.stop(ctx)
                        } else {
                            // si ya hay música reproduciéndose, puedes arrancarlo al instante (opcional)
                            val state = com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus.playbackState.value
                            val pkg = com.bryanguerra.dynamicislandmusic.data.media.MediaSessionBus.activePackage.value
                            if ((state == android.media.session.PlaybackState.STATE_PLAYING ||
                                        state == android.media.session.PlaybackState.STATE_BUFFERING) &&
                                pkg == com.bryanguerra.dynamicislandmusic.util.Constants.TARGET_PLAYER_PKG
                            ) {
                                com.bryanguerra.dynamicislandmusic.services.IslandForegroundService.start(ctx)
                            }
                        }
                    }
                )*/

            }
        }

        // --- Permisos ---
        PermissionCard(
            title = "Dibujar sobre otras apps",
            desc = "Necesario para mostrar la isla sobre otras aplicaciones.",
            granted = ui.overlayGranted,
            onGrant = { ctx.startActivity(PermissionsHelper.overlaySettingsIntent(ctx)) }
        )

        PermissionCard(
            title = "Acceso a notificaciones",
            desc = "Necesario para leer/controlar la sesión de música.",
            granted = ui.notifListenerGranted,
            onGrant = { ctx.startActivity(PermissionsHelper.notificationListenerSettingsIntent()) }
        )

        PermissionCard(
            title = "Acceso de uso",
            desc = "Para ocultar la isla cuando RiMusic está en primer plano.",
            granted = ui.usageAccessGranted,
            onGrant = { ctx.startActivity(PermissionsHelper.usageAccessSettingsIntent()) }
        )

        if (PermissionsHelper.needsPostNotifications()) {
            PermissionCard(
                title = "Permiso de notificaciones",
                desc = "Recomendado para la notificación del servicio en Android 13+.",
                granted = ui.postNotificationsGranted,
                onGrant = {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }

        // Estado global de permisos
        PermissionsSummary(ui)
    }
}

@Composable
private fun PermissionCard(
    title: String,
    desc: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Card {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodySmall)
                val status = if (granted) "Concedido" else "Falta conceder"
                val color =
                    if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(status, color = color, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(12.dp))
            val buttonText = if (granted) "Abrir" else "Conceder"
            Button(onClick = onGrant) { Text(buttonText) }
        }
    }
}

@Composable
private fun PermissionsSummary(ui: SettingsUiState) {
    val allGranted =
        ui.overlayGranted && ui.notifListenerGranted && ui.usageAccessGranted &&
                ui.postNotificationsGranted

    val text = if (allGranted) {
        "Permisos OK. La isla funcionará cuando reproduzcas música."
    } else {
        "Faltan permisos. Concede los que aparecen en rojo."
    }

    val color =
        if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
}