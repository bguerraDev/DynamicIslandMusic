package com.bryanguerra.dynamicislandmusic.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.core.content.getSystemService

/**
 * Utilidades para saber si el sistema permite blur de ventana
 * (S+ con "Allow window blurs" activo y soporte del dispositivo).
 */
object BlurSupport {

    /** ¿El SO y el usuario permiten blur entre ventanas? */
    fun isCrossWindowBlurEnabled(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val wm: WindowManager? = ctx.getSystemService()
        // true únicamente cuando el dispositivo lo soporta y el dev-option NO lo tiene desactivado
        return wm?.isCrossWindowBlurEnabled == true
    }

    /** ¿Tiene sentido intentar blur de ventana en este dispositivo? */
    fun isCrossWindowBlurPossible(ctx: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Intent a “Opciones de desarrollador” para que el usuario active el blur. */
    fun developerOptionsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
