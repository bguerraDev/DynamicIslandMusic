// ColorExtractor.kt
package com.bryanguerra.dynamicislandmusic.overlay

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

data class DominantColors(
    val bg: Color,
    val onBg: Color,
    val accent: Color
)

object ColorExtractor {
    fun extract(bitmap: Bitmap?): DominantColors {
        if (bitmap == null) return fallback()
        val p = Palette.from(bitmap).clearFilters().generate()

        val dominant = p.getDominantColor(0xFF222222.toInt())
        val vib = p.getVibrantColor(dominant)
        val darkVib = p.getDarkVibrantColor(vib)
        val bg = Color(darkVib)
        val on = if (isDark(bg)) Color(0xFFF5F5F5) else Color(0xFF121212)
        val accent = Color(vib)
        return DominantColors(bg = bg, onBg = on, accent = accent)
    }

    private fun isDark(c: Color): Boolean {
        // luminancia aproximada
        val r = c.red; val g = c.green; val b = c.blue
        val lum = 0.2126*r + 0.7152*g + 0.0722*b
        return lum < 0.5
    }

    private fun fallback() = DominantColors(
        bg = Color(0xFF1E1E1E),
        onBg = Color(0xFFF5F5F5),
        accent = Color(0xFF888888)
    )
}