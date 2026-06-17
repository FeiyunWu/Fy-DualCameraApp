package com.fydualcamera.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fydualcamera.app.db.AppDatabase
import com.fydualcamera.app.db.MediaEntity
import com.fydualcamera.app.layout.LayoutMode
import com.fydualcamera.app.model.PresetTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val mediaDao = db.mediaDao()

    private val _layoutMode = MutableStateFlow(LayoutMode.PIP)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration

    private val _selectedResolution = MutableStateFlow("1080p")
    val selectedResolution: StateFlow<String> = _selectedResolution

    private val _isDualCameraSupported = MutableStateFlow(true)
    val isDualCameraSupported: StateFlow<Boolean> = _isDualCameraSupported

    val allMedia = mediaDao.getAllMedia()
    val totalStorageSize = mediaDao.getTotalSize()

    private val _presetTemplates = MutableStateFlow(PresetTemplate.defaults())
    val presetTemplates: StateFlow<List<PresetTemplate>> = _presetTemplates

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
    }

    fun startRecording() {
        _isRecording.value = true
        _isPaused.value = false
    }

    fun pauseRecording() {
        _isPaused.value = true
    }

    fun resumeRecording() {
        _isPaused.value = false
    }

    fun stopRecording() {
        _isRecording.value = false
        _isPaused.value = false
        _recordingDuration.value = 0L
    }

    fun setResolution(resolution: String) {
        _selectedResolution.value = resolution
    }

    fun deleteMedia(media: MediaEntity) {
        viewModelScope.launch {
            mediaDao.delete(media)
        }
    }

    fun insertMedia(media: MediaEntity) {
        viewModelScope.launch {
            mediaDao.insert(media)
        }
    }
}
