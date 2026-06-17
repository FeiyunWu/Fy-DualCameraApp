package com.fydualcamera.app.camera

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Size
import android.view.Display
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fydualcamera.app.layout.LayoutMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DualCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var frontCamera: Camera? = null
    private var backCamera: Camera? = null
    private var frontPreview: Preview? = null
    private var backPreview: Preview? = null
    private var frontVideoCapture: VideoCapture<*>? = null
    private var backVideoCapture: VideoCapture<*>? = null

    var frontPreviewView: PreviewView? = null
    var backPreviewView: PreviewView? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration

    private val _isDualCameraSupported = MutableStateFlow(true)
    val isDualCameraSupported: StateFlow<Boolean> = _isDualCameraSupported

    private var quality: Quality = Quality.FHD
    private var layoutMode: LayoutMode = LayoutMode.PIP

    fun setQuality(level: Quality) {
        quality = level
    }

    fun setLayoutMode(mode: LayoutMode) {
        layoutMode = mode
    }

    fun startCameras() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameras(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameras(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()

        val display = getDisplayCompat()

        val frontSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val backSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val qualitySelector = QualitySelector.from(quality)

        frontPreview = Preview.Builder()
            .setTargetResolution(Size(1920, 1080))
            .setTargetRotation(display?.rotation ?: android.view.Surface.ROTATION_0)
            .build()

        backPreview = Preview.Builder()
            .setTargetResolution(Size(1920, 1080))
            .setTargetRotation(display?.rotation ?: android.view.Surface.ROTATION_0)
            .build()

        frontVideoCapture = VideoCapture.withOutput(
            Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
        )
        backVideoCapture = VideoCapture.withOutput(
            Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
        )

        try {
            if (frontPreviewView != null) {
                frontPreview?.setSurfaceProvider(frontPreviewView!!.surfaceProvider)
                frontCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    frontSelector,
                    frontPreview,
                    frontVideoCapture
                )
            }

            if (backPreviewView != null) {
                backPreview?.setSurfaceProvider(backPreviewView!!.surfaceProvider)
                backCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    backSelector,
                    backPreview,
                    backVideoCapture
                )
            }

            _isDualCameraSupported.value = true
        } catch (e: Exception) {
            _isDualCameraSupported.value = false
            try {
                if (frontPreviewView != null) {
                    frontCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        frontSelector,
                        frontPreview,
                        frontVideoCapture
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun getDisplayCompat(): Display? {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        return manager?.getDisplay(0)
    }

    fun startRecording(outputPathFront: String, outputPathBack: String) {
        _isRecording.value = true
    }

    fun stopRecording() {
        _isRecording.value = false
    }

    fun takePhoto() {

    }

    fun switchToSingleCamera(position: CameraPosition) {

    }

    fun release() {
        cameraExecutor.shutdown()
    }
}
