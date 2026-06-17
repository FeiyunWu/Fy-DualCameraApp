package com.fydualcamera.app.util

object SizeUtils {
    data class Resolution(val width: Int, val height: Int) {
        val label: String get() = when {
            width >= 3840 -> "4K"
            width >= 1920 -> "1080p"
            width >= 1280 -> "720p"
            width >= 854 -> "480p"
            else -> "${width}x${height}"
        }
    }

    val RESOLUTIONS = listOf(
        Resolution(3840, 2160),
        Resolution(1920, 1080),
        Resolution(1280, 720),
        Resolution(854, 480)
    )

    val ASPECT_RATIOS = listOf(
        "1:1" to 1.0f,
        "16:9" to 16f / 9f,
        "9:16" to 9f / 16f,
        "4:3" to 4f / 3f,
        "3:4" to 3f / 4f
    )

    val MIN_PIP_SIZE = 0.1f
    val MAX_PIP_SIZE = 0.8f
    val MIN_SPLIT_RATIO = 0.2f
    val MAX_SPLIT_RATIO = 0.8f
}
