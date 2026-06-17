package com.fydualcamera.app.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.util.Size
import android.view.Display
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.fydualcamera.app.db.MediaEntity
import com.fydualcamera.app.layout.LayoutMode
import com.fydualcamera.app.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

@SuppressLint("MissingPermission")
class DualCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null

    private var frontCamera: Camera? = null
    private var backCamera: Camera? = null
    private var frontPreview: Preview? = null
    private var backPreview: Preview? = null
    private var frontVideoCapture: VideoCapture<Recorder>? = null
    private var backVideoCapture: VideoCapture<Recorder>? = null
    private var frontImageCapture: ImageCapture? = null
    private var backImageCapture: ImageCapture? = null

    var frontPreviewView: PreviewView? = null
        set(value) {
            field = value
            rebindIfReady()
        }

    var backPreviewView: PreviewView? = null
        set(value) {
            field = value
            rebindIfReady()
        }

    private var frontRecording: Recording? = null
    private var backRecording: Recording? = null
    private var isRecordingActive = false

    var onMediaSaved: ((MediaEntity) -> Unit)? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isDualCameraSupported = MutableStateFlow(true)
    val isDualCameraSupported: StateFlow<Boolean> = _isDualCameraSupported

    private var layoutMode: LayoutMode = LayoutMode.PIP

    fun setLayoutMode(mode: LayoutMode) {
        layoutMode = mode
    }

    fun startCameras() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            rebindIfReady()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun rebindIfReady() {
        val provider = cameraProvider ?: return
        if (frontPreviewView == null && backPreviewView == null) return
        bindCameras(provider)
    }

    private fun bindCameras(provider: ProcessCameraProvider) {
        provider.unbindAll()

        val display = getDisplayCompat()
        val rotation = display?.rotation ?: android.view.Surface.ROTATION_0

        val frontSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val backSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        frontPreview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()

        backPreview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()

        frontImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .build()

        backImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .build()

        val qualitySelector = QualitySelector.from(
            Quality.FHD,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
        )

        frontVideoCapture = VideoCapture.withOutput(
            Recorder.Builder().setQualitySelector(qualitySelector).build()
        )
        backVideoCapture = VideoCapture.withOutput(
            Recorder.Builder().setQualitySelector(qualitySelector).build()
        )

        var frontBound = false
        var backBound = false

        if (frontPreviewView != null) {
            val pv = frontPreview
            val vc = frontVideoCapture
            val ic = frontImageCapture
            if (pv != null && vc != null && ic != null) {
                try {
                    pv.setSurfaceProvider(frontPreviewView!!.surfaceProvider)
                    frontCamera = provider.bindToLifecycle(
                        lifecycleOwner, frontSelector, pv, vc, ic
                    )
                    frontBound = true
                } catch (e: Exception) {
                    Log.e("DualCamera", "Front all-3 failed: ${e.message}")
                    frontVideoCapture = null
                    frontImageCapture = null
                    try {
                        pv.setSurfaceProvider(frontPreviewView!!.surfaceProvider)
                        frontCamera = provider.bindToLifecycle(
                            lifecycleOwner, frontSelector, pv
                        )
                        frontBound = true
                    } catch (e2: Exception) {
                        Log.e("DualCamera", "Front preview failed: ${e2.message}")
                    }
                }
            }
        }

        if (backPreviewView != null) {
            val pv = backPreview
            val vc = backVideoCapture
            val ic = backImageCapture
            if (pv != null && vc != null && ic != null) {
                try {
                    pv.setSurfaceProvider(backPreviewView!!.surfaceProvider)
                    backCamera = provider.bindToLifecycle(
                        lifecycleOwner, backSelector, pv, vc, ic
                    )
                    backBound = true
                } catch (e: Exception) {
                    Log.e("DualCamera", "Back all-3 failed: ${e.message}")
                    backVideoCapture = null
                    backImageCapture = null
                    try {
                        pv.setSurfaceProvider(backPreviewView!!.surfaceProvider)
                        backCamera = provider.bindToLifecycle(
                            lifecycleOwner, backSelector, pv
                        )
                        backBound = true
                    } catch (e2: Exception) {
                        Log.e("DualCamera", "Back preview failed: ${e2.message}")
                    }
                }
            }
        }

        _isDualCameraSupported.value = frontBound && backBound
    }

    private fun getDisplayCompat(): Display? {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        return manager?.getDisplay(0)
    }

    fun takePhoto() {
        if (frontImageCapture == null && backImageCapture == null) return

        val dir = FileUtils.getPhotoOutputDir(context)
        val executor = ContextCompat.getMainExecutor(context)

        frontImageCapture?.let { ic ->
            val view = frontPreviewView
            if (view != null) {
                try {
                    val file = File(dir, FileUtils.generatePhotoFileName("IMG_FRONT"))
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    ic.takePicture(options, executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val entity = MediaEntity(
                                    fileName = file.name,
                                    filePath = file.absolutePath,
                                    type = "photo_front",
                                    sizeBytes = file.length()
                                )
                                Log.d("DualCamera", "Photo saved: ${file.absolutePath}")
                                onMediaSaved?.invoke(entity)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                Log.e("DualCamera", "Front photo error: ${exception.message}")
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("DualCamera", "Front photo exception: ${e.message}")
                }
            }
        }

        backImageCapture?.let { ic ->
            val view = backPreviewView
            if (view != null) {
                try {
                    val file = File(dir, FileUtils.generatePhotoFileName("IMG_BACK"))
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    ic.takePicture(options, executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val entity = MediaEntity(
                                    fileName = file.name,
                                    filePath = file.absolutePath,
                                    type = "photo_back",
                                    sizeBytes = file.length()
                                )
                                Log.d("DualCamera", "Photo saved: ${file.absolutePath}")
                                onMediaSaved?.invoke(entity)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                Log.e("DualCamera", "Back photo error: ${exception.message}")
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("DualCamera", "Back photo exception: ${e.message}")
                }
            }
        }
    }

    fun startRecording() {
        if (isRecordingActive) return
        val frontVc = frontVideoCapture
        val backVc = backVideoCapture
        val frontView = frontPreviewView
        val backView = backPreviewView
        if (frontVc == null && backVc == null) return

        val videoDir = FileUtils.getVideoOutputDir(context)
        val executor = ContextCompat.getMainExecutor(context)

        frontVc?.let { vc ->
            val view = frontView
            if (view != null) {
                try {
                    val file = File(videoDir, FileUtils.generateVideoFileName("VID_FRONT"))
                    val options = FileOutputOptions.Builder(file).build()
                    val pending = vc.output.prepareRecording(context, options)
                    frontRecording = pending.start(executor, Consumer<VideoRecordEvent> { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            Log.d("DualCamera", "Front video finalize: error=${event.error}")
                            onMediaSaved?.invoke(MediaEntity(
                                fileName = file.name,
                                filePath = file.absolutePath,
                                type = "video_front",
                                sizeBytes = file.length()
                            ))
                        }
                    })
                } catch (e: Exception) {
                    Log.e("DualCamera", "Front recording start failed: ${e.message}")
                }
            }
        }

        backVc?.let { vc ->
            val view = backView
            if (view != null) {
                try {
                    val file = File(videoDir, FileUtils.generateVideoFileName("VID_BACK"))
                    val options = FileOutputOptions.Builder(file).build()
                    val pending = vc.output.prepareRecording(context, options).withAudioEnabled()
                    backRecording = pending.start(executor, Consumer<VideoRecordEvent> { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            Log.d("DualCamera", "Back video finalize: error=${event.error}")
                            onMediaSaved?.invoke(MediaEntity(
                                fileName = file.name,
                                filePath = file.absolutePath,
                                type = "video_back",
                                sizeBytes = file.length()
                            ))
                        }
                    })
                } catch (e: Exception) {
                    Log.e("DualCamera", "Back recording start failed: ${e.message}")
                }
            }
        }

        isRecordingActive = true
        _isRecording.value = true
    }

    fun stopRecording() {
        if (!isRecordingActive) return
        try {
            frontRecording?.stop()
        } catch (e: Exception) {
            Log.e("DualCamera", "Stop front recording error: ${e.message}")
        }
        try {
            backRecording?.stop()
        } catch (e: Exception) {
            Log.e("DualCamera", "Stop back recording error: ${e.message}")
        }
        frontRecording = null
        backRecording = null
        isRecordingActive = false
        _isRecording.value = false
    }

    fun switchToSingleCamera(position: CameraPosition) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val selector = when (position) {
            CameraPosition.FRONT -> CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
            CameraPosition.BACK -> CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        }

        val preview = Preview.Builder()
            .setTargetResolution(Size(1920, 1080))
            .build()

        val view = when (position) {
            CameraPosition.FRONT -> frontPreviewView
            CameraPosition.BACK -> backPreviewView
        }

        if (view != null) {
            try {
                preview.setSurfaceProvider(view.surfaceProvider)
                provider.bindToLifecycle(lifecycleOwner, selector, preview)
            } catch (_: Exception) { }
        }
    }

    fun release() {
        stopRecording()
    }
}
