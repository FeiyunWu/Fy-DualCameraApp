package com.fydualcamera.app.model

import com.fydualcamera.app.layout.LayoutConfig
import com.fydualcamera.app.layout.LayoutMode

data class PresetTemplate(
    val id: Long = 0,
    val name: String,
    val layoutMode: LayoutMode,
    val splitRatio: Float = 0.5f,
    val frontOnTop: Boolean = false,
    val frontWidthRatio: Float = 0.3f,
    val frontHeightRatio: Float = 0.3f,
    val cornerRadius: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toLayoutConfig(): LayoutConfig {
        return LayoutConfig(
            mode = layoutMode,
            splitRatio = splitRatio,
            frontOnTop = frontOnTop,
            cornerRadius = cornerRadius
        )
    }

    companion object {
        fun defaults(): List<PresetTemplate> = listOf(
            PresetTemplate(name = "经典画中画", layoutMode = LayoutMode.PIP),
            PresetTemplate(name = "左右分屏", layoutMode = LayoutMode.LEFT_RIGHT, splitRatio = 0.5f),
            PresetTemplate(name = "上下分屏", layoutMode = LayoutMode.TOP_BOTTOM, splitRatio = 0.5f),
            PresetTemplate(name = "前置大窗", layoutMode = LayoutMode.PIP, frontOnTop = true)
        )
    }
}
