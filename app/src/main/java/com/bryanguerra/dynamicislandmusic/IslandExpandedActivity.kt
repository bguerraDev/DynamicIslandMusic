package com.bryanguerra.dynamicislandmusic

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bryanguerra.dynamicislandmusic.data.settings.SettingsRepository
import com.bryanguerra.dynamicislandmusic.ui.island.MusicPopUp
import com.bryanguerra.dynamicislandmusic.domain.overlay.ShowIslandUseCase
import com.bryanguerra.dynamicislandmusic.domain.settings.WaveStyle
import com.bryanguerra.dynamicislandmusic.util.BlurSupport
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IslandExpandedActivity : ComponentActivity() {

    @Inject
    lateinit var showIslandUseCase: ShowIslandUseCase // ocultar pill al abrir

    @Inject lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Decor fits. Evita que el sistema inserte paddings
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Inmersivo: oculta status + nav, con gesto para mostrarlas temporalmente (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(
                WindowInsetsCompat.Type.statusBars() or
                        WindowInsetsCompat.Type.navigationBars()
            )
        } else { // For versions older than Android 11
            // Use deprecated flags for older versions
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    // Controls immersive mode.
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            // Hide the nav bar and status bar
                            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        // 1) Crea el contenido primero (esto instala el decor)
        setContent {
            val wave by settingsRepo.waveStyleFlow.collectAsState(initial = WaveStyle.Classic)
            ExpandedBackdrop(
                onDismiss = { finish() },
                selectedWave = wave
            )
        }
        // 2) Aplicar el blur (API 31+), después de tener el decor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && BlurSupport.isCrossWindowBlurEnabled(this)) {
            window.decorView.post {
                runCatching {
                    window.setBackgroundBlurRadius(14)
                    // Si quieres un toque de oscurecido, muy bajo:
                    //@Suppress("DEPRECATION")
                    //window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    // TODO PROBAR. Algunos OEM responden mejor también a:
                    //window.attributes = window.attributes.apply { dimAmount = 0f; blurBehindRadius = 60 }
                }.onFailure {
                    Log.w("IslandExpandedActivity", "Window blur not applied", it)
                }
            }
        }

        // This callback will only be called when MyFragment is at least Started.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
    }

    override fun onDestroy() {
        // si ocultaste la pill al abrir, vuelve a mostrarla
        runCatching { showIslandUseCase() }
        super.onDestroy()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandedBackdrop(
    onDismiss: () -> Unit,
    selectedWave: WaveStyle
) {
    // Fondo clicable para cerrar
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    )

    // Coloca tu popup encima (centrado/padding como en IslandRoot cuando expanded)

    Box(
        // Opción A: to-do en una llamada
        Modifier
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .padding(start = 12.dp, top = 12.dp, end = 12.dp)

        // Opción B: encadenado
        // Modifier.padding(horizontal = 12.dp).padding(top = 12.dp)
    ) {
        MusicPopUp(
            onSwipeUpClose = onDismiss,
            selectedWave = selectedWave
        )
    }
}