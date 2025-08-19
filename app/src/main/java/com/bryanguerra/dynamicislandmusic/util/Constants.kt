package com.bryanguerra.dynamicislandmusic.util

object Constants {
    const val TARGET_PLAYER_PKG = "it.fast4x.rimusic"
    const val INACTIVITY_TIMEOUT_MS = 45_000L   // 45 s tras PAUSE/STOP
    const val NOTIF_CHANNEL_ID = "island_overlay"
    const val NOTIF_CHANNEL_NAME = "Dynamic Island"
    const val NOTIF_ID = 1001

    data class IslandSizePreset(
        val pillWidthDp: Int,
        val pillHeightDp: Int,
        val expandedWidthDp: Int,
        val expandedHeightDp: Int,
        val cornerRadiusDp: Int,
        val topOffsetDp: Int,
        val sideMarginDp: Int
    )

    val PRESET_PIXEL_8_PRO = IslandSizePreset(
        pillWidthDp = 118,
        pillHeightDp = 38,
        expandedWidthDp = 340,
        expandedHeightDp = 200,
        cornerRadiusDp = 22,
        topOffsetDp = 12,
        sideMarginDp = 16
    )
}