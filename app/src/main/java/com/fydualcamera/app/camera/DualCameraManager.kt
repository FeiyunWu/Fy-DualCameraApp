package com.fydualcamera.app.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.MediaStore
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
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fydualcamera.app.db.MediaEntity
import com.fydualcamera.app.layout.LayoutMode
import com.fydualcamera.app.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.util.Consumer
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class DualCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

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

    var onMediaSaved: ((MediaEntity) -> Unit)? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

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

        val qualitySelector = QualitySelector.from(Quality.FHD, Quality.HD, Quality.SD)

        frontPreview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()

        backPreview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()

        frontVideoCapture = VideoCapture.withOutput(
            Recorder.Builder().setQualitySelector(qualitySelector).build()
        )
        backVideoCapture = VideoCapture.withOutput(
            Recorder.Builder().setQualitySelector(qualitySelector).build()
        )

        frontImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .build()

        backImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .build()

        var frontBound = false
        var backBound = false

        if (frontPreviewView != null) {
            try {
                val pv = frontPreview
                val vc = frontVideoCapture
                val ic = frontImageCapture
                if (pv != null && vc != null && ic != null) {
                    pv.setSurfaceProvider(frontPreviewView!!.surfaceProvider)
                    frontCamera = provider.bindToLifecycle(
                        lifecycleOwner, frontSelector, pv, vc, ic
                    )
                    frontBound = true
                }
            } catch (e: Exception) {
                Log.e("DualCamera", "Front bind (all) failed: ${e.message}")
                frontVideoCapture = null
                frontImageCapture = null
                try {
                    val pv = frontPreview
                    if (pv != null && frontPreviewView != null) {
                        pv.setSurfaceProvider(frontPreviewView!!.surfaceProvider)
                        frontCamera = provider.bindToLifecycle(
                            lifecycleOwner, frontSelector, pv
                        )
                        frontBound = true
                        Log.d("DualCamera", "Front bound with Preview only")
                    }
                } catch (e2: Exception) {
                    Log.e("DualCamera", "Front bind (preview only) failed: ${e2.message}")
                }
            }
        }

        if (backPreviewView != null) {
            try {
                val pv = backPreview
                val vc = backVideoCapture
                val ic = backImageCapture
                if (pv != null && vc != null && ic != null) {
                    pv.setSurfaceProvider(backPreviewView!!.surfaceProvider)
                    backCamera = provider.bindToLifecycle(
                        lifecycleOwner, backSelector, pv, vc, ic
                    )
                    backBound = true
                }
            } catch (e: Exception) {
                Log.e("DualCamera", "Back bind failed: ${e.message}")
            }
        }

        if (!frontBound && backPreviewView != null) {
            try {
                val pv = backPreview
                val vc = backVideoCapture
                val ic = backImageCapture
                if (pv != null && vc != null && ic != null) {
                    pv.setSurfaceProvider(backPreviewView!!.surfaceProvider)
                    backCamera = provider.bindToLifecycle(
                        lifecycleOwner, backSelector, pv, vc, ic
                    )
                    backBound = true
                }
            } catch (e: Exception) {
                Log.e("DualCamera", "Fallback back bind failed: ${e.message}")
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

        val executor = ContextCompat.getMainExecutor(context)

        takePhotoSingle(frontImageCapture, "IMG_FRONT", "photo_front", executor)
        takePhotoSingle(backImageCapture, "IMG_BACK", "photo_back", executor)
    }

    private fun takePhotoSingle(
        imageCapture: ImageCapture?,
        prefix: String,
        type: String,
        executor: Executor
    ) {
        if (imageCapture == null) return

        try {
            val fileName = FileUtils.generatePhotoFileName(prefix)
            val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/DualCamera")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return
                ImageCapture.OutputFileOptions.Builder(context.contentResolver, uri).build()
            } else {
                val dir = File(context.filesDir, "DualCamera/Photos")
                dir.mkdirs()
                val file = File(dir, fileName)
                ImageCapture.OutputFileOptions.Builder(file).build()
            }

            imageCapture.takePicture(
                options, executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri
                        val path = savedUri?.toString() ?: ""
                        val entity = MediaEntity(
                            fileName = fileName,
                            filePath = path,
                            type = type,
                            sizeBytes = 0L
                        )
                        onMediaSaved?.invoke(entity)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("DualCamera", "takePhoto $type error: ${exception.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("DualCamera", "takePhoto $type failed: ${e.message}")
        }
    }

    fun startRecording() {
        val frontVc = frontVideoCapture
        val backVc = backVideoCapture
        val frontView = frontPreviewView
        val backView = backPreviewView
        if (frontVc == null && backVc == null) return

        val executor = ContextCompat.getMainExecutor(context)

        if (frontVc != null && frontView != null) {
            startRecordingSingle(frontVc, "VID_FRONT", "video_front", executor, false)
        }

        if (backVc != null && backView != null) {
            startRecordingSingle(backVc, "VID_BACK", "video_back", executor, true)
        }

        _isRecording.value = true
    }

    private fun startRecordingSingle(
        videoCapture: VideoCapture<Recorder>,
        prefix: String,
        type: String,
        executor: Executor,
        withAudio: Boolean
    ) {
        try {
            val recorder = videoCapture.output
            val videoFileName = FileUtils.generateVideoFileName(prefix)

            val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/DualCamera")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return
                MediaStoreOutputOptions.Builder(context.contentResolver, uri).build()
            } else {
                val dir = File(context.filesDir, "DualCamera/Videos")
                dir.mkdirs()
                val file = File(dir, videoFileName)
                FileOutputOptions.Builder(file).build()
            }

            val pending = recorder.prepareRecording(context, options)
            val pendingWithAudio = if (withAudio) pending.withAudioEnabled() else pending

            val isFront = type == "video_front"
            val recording = pendingWithAudio.start(executor, Consumer<VideoRecordEvent> { event ->
                if (event is VideoRecordEvent.Finalize) {
                    val savedUri = event.outputResults?.outputUri
                    val path = savedUri?.toString() ?: ""
                    onMediaSaved?.invoke(MediaEntity(
                        fileName = videoFileName,
                        filePath = path,
                        type = type,
                        sizeBytes = 0L
                    ))
                }
            })

            if (isFront) {
                frontRecording = recording
            } else {
                backRecording = recording
            }
        } catch (e: Exception) {
            Log.e("DualCamera", "startRecording $type failed: ${e.message}")
        }
    }

    fun stopRecording() {
        try { frontRecording?.stop() } catch (_: Exception) { }
        try { backRecording?.stop() } catch (_: Exception) { }
        frontRecording = null
        backRecording = null
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
        try { frontRecording?.stop() } catch (_: Exception) { }
        try { backRecording?.stop() } catch (_: Exception) { }
        frontRecording = null
        backRecording = null
        cameraExecutor.shutdown()
    }
}
