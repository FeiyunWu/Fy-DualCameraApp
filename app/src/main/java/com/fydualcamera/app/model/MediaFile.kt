package com.fydualcamera.app.model

import android.net.Uri

data class MediaFile(
    val id: Long = 0,
    val uri: Uri,
    val path: String,
    val fileName: String,
    val type: MediaType,
    val durationMs: Long = 0,
    val sizeBytes: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val pairedFrontUri: Uri? = null,
    val pairedBackUri: Uri? = null,
    val isComposed: Boolean = false
)

enum class MediaType {
    VIDEO_COMPOSED,
    VIDEO_FRONT,
    VIDEO_BACK,
    PHOTO_COMPOSED,
    PHOTO_FRONT,
    PHOTO_BACK
}
