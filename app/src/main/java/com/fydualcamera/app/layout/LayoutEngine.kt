package com.fydualcamera.app.layout

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset

data class WindowRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun toRectF(): RectF = RectF(left, top, right, bottom)

    fun contains(point: Offset): Boolean {
        return point.x in left..right && point.y in top..bottom
    }

    fun scale(factor: Float): WindowRect {
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val halfW = width * factor / 2f
        val halfH = height * factor / 2f
        return WindowRect(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
    }

    fun translate(dx: Float, dy: Float): WindowRect {
        return WindowRect(left + dx, top + dy, right + dx, bottom + dy)
    }
}

data class LayoutConfig(
    val mode: LayoutMode = LayoutMode.PIP,
    val frontWindow: WindowRect = WindowRect(0.7f, 0.05f, 0.95f, 0.3f),
    val backWindow: WindowRect = WindowRect(0f, 0f, 1f, 1f),
    val splitRatio: Float = 0.5f,
    val frontOnTop: Boolean = false,
    val cornerRadius: Float = 0f
)

class LayoutEngine {
    fun calculateLayout(
        config: LayoutConfig,
        containerWidth: Float,
        containerHeight: Float
    ): Pair<WindowRect, WindowRect> {
        return when (config.mode) {
            LayoutMode.PIP -> calculatePipLayout(config, containerWidth, containerHeight)
            LayoutMode.LEFT_RIGHT -> calculateSplitLayout(config, containerWidth, containerHeight, isHorizontal = true)
            LayoutMode.TOP_BOTTOM -> calculateSplitLayout(config, containerWidth, containerHeight, isHorizontal = false)
            LayoutMode.FREE -> config.frontWindow to config.backWindow
        }
    }

    private fun calculatePipLayout(
        config: LayoutConfig,
        cw: Float,
        ch: Float
    ): Pair<WindowRect, WindowRect> {
        val back = WindowRect(0f, 0f, cw, ch)
        val pipW = cw * 0.3f
        val pipH = ch * 0.3f
        val front = WindowRect(cw - pipW - 16f, 16f, cw - 16f, pipH + 16f)
        return if (config.frontOnTop) front to back else back to front
    }

    private fun calculateSplitLayout(
        config: LayoutConfig,
        cw: Float,
        ch: Float,
        isHorizontal: Boolean
    ): Pair<WindowRect, WindowRect> {
        val ratio = config.splitRatio.coerceIn(0.2f, 0.8f)
        return if (isHorizontal) {
            val splitX = cw * ratio
            val front = WindowRect(0f, 0f, splitX, ch)
            val back = WindowRect(splitX, 0f, cw, ch)
            front to back
        } else {
            val splitY = ch * ratio
            val front = WindowRect(0f, 0f, cw, splitY)
            val back = WindowRect(0f, splitY, cw, ch)
            front to back
        }
    }
}
