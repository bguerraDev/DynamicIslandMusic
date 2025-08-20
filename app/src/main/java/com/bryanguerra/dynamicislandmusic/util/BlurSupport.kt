// utils/BlurSupport.kt
package com.bryanguerra.dynamicislandmusic.util

import android.content.Context
import android.os.Build
import android.view.WindowManager

object BlurSupport {
    fun isSystemBlurEnabled(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return try {
            val wm = ctx.getSystemService(WindowManager::class.java)
            wm?.isCrossWindowBlurEnabled == true
        } catch (_: Throwable) { false }
    }
}
