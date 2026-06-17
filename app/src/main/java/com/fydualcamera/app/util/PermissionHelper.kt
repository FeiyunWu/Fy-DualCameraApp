package com.fydualcamera.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {
    val REQUIRED_CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    val REQUIRED_AUDIO_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    val REQUIRED_STORAGE_PERMISSIONS: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

    val ALL_REQUIRED_PERMISSIONS: Array<String>
        get() = REQUIRED_CAMERA_PERMISSIONS +
                REQUIRED_AUDIO_PERMISSIONS +
                REQUIRED_STORAGE_PERMISSIONS

    fun hasCameraPermission(context: Context): Boolean {
        return REQUIRED_CAMERA_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAudioPermission(context: Context): Boolean {
        return REQUIRED_AUDIO_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasStoragePermission(context: Context): Boolean {
        return REQUIRED_STORAGE_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAllPermissions(context: Context): Boolean {
        return ALL_REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
