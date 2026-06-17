package com.fydualcamera.app.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    private const val APP_DIR = "DualCamera"
    private const val VIDEO_DIR = "Videos"
    private const val PHOTO_DIR = "Photos"
    private const val COMPOSED_DIR = "Composed"

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())

    fun getAppOutputDir(context: Context): File {
        val dir = File(context.filesDir, APP_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getVideoOutputDir(context: Context): File {
        val dir = File(getAppOutputDir(context), VIDEO_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPhotoOutputDir(context: Context): File {
        val dir = File(getAppOutputDir(context), PHOTO_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getComposedOutputDir(context: Context): File {
        val dir = File(getAppOutputDir(context), COMPOSED_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateVideoFileName(prefix: String = "VID"): String {
        val timestamp = dateFormat.format(Date())
        return "${prefix}_${timestamp}.mp4"
    }

    fun generatePhotoFileName(prefix: String = "IMG"): String {
        val timestamp = dateFormat.format(Date())
        return "${prefix}_${timestamp}.jpg"
    }

    fun getFreeStorageBytes(): Long {
        val stat = Environment.getExternalStorageDirectory()
        return stat.freeSpace
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            "${h}:${"%02d".format(m)}:${"%02d".format(s)}"
        } else {
            "${"%02d".format(m)}:${"%02d".format(s)}"
        }
    }
}
