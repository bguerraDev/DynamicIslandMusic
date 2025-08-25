package com.bryanguerra.dynamicislandmusic.ui.settings

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bryanguerra.dynamicislandmusic.domain.settings.WaveStyle
import com.bryanguerra.dynamicislandmusic.util.BlurSupport
import com.bryanguerra.dynamicislandmusic.util.PermissionsHelper
import com.bryanguerra.dynamicislandmusic.viewmodel.SettingsUiState
import com.bryanguerra.dynamicislandmusic.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {

    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refreshPermissions() }

    // Re-evaluar permisos al volver a la app (ej. tras abrir Ajustes)
    LaunchedEffect(Unit) {
        // pequeño "poll" porque los Settings son externos
        while (true) {
            vm.refreshPermissions()
            kotlinx.coroutines.delay(1000)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Hace que el contenido respete satatus+nav (safe areas)
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {

            // Los TopAppBar de Material3 ya mapean status bars
            CenterAlignedTopAppBar(
                title = { Text("Ajustes") },
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { inner ->
        SettingsContent(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp), // margen en bordes
            vm = vm,
            ui = ui,
            notifLauncher = notifLauncher,
            ctx = ctx
        )
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier,
    vm: SettingsViewModel,
    ui: SettingsUiState,
    notifLauncher: ActivityResultLauncher<String>,
    ctx: Context
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (ui.showBlurWarning) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Difuminado desactivado",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Para ver el blur del fondo, activa «Permitir difuminar ventanas» " +
                                "en Opciones de desarrollador (o desactiva «Desactivar efectos de desenfoque»).",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = {
                            val intent = BlurSupport.developerOptionsIntent()
                            runCatching { ctx.startActivity(intent) }
                        }) { Text("Abrir desarrollador") }
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
                // Switch activar/desactivar isla
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
        // ExposedDropdownMenuBox
        WaveStyleSelector(
            current = ui.waveStyle,
            onSelect = { vm.setWaveStyle(it) }
        )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveStyleSelector(
    current: WaveStyle,
    onSelect: (WaveStyle) -> Unit
) {
    val options = WaveStyle.entries
    var expanded by remember { mutableStateOf(false) }

    // Caja
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Animación de ondas", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Elige el estilo de ondas que se mostrará en la pill y en la vista expandida",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = current.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Estilo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { style ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Mini preview Lottie (frame 0)
                                    MiniLottiePreview(rawRes = style.rawRes)
                                    Spacer(Modifier.width(12.dp))
                                    Text(style.label)
                                }
                            },
                            onClick = {
                                onSelect(style)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniLottiePreview(@androidx.annotation.RawRes rawRes: Int) {
    // Mostramos el primer frame como icono
    val comp by com.airbnb.lottie.compose.rememberLottieComposition(
        com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(rawRes)
    )
    com.airbnb.lottie.compose.LottieAnimation(
        composition = comp,
        progress = { 0f },
        modifier = Modifier.size(28.dp)
    )
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