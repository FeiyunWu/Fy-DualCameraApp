package com.fydualcamera.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_files")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val type: String,
    val durationMs: Long = 0,
    val sizeBytes: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val pairedFrontPath: String? = null,
    val pairedBackPath: String? = null,
    val isComposed: Boolean = false,
    val width: Int = 0,
    val height: Int = 0
)
