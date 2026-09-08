package com.fydualcamera.app.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fydualcamera.app.layout.LayoutMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "DualCamera"

@SuppressLint("MissingPermission")
class DualCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null

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

    private val _isDualCameraSupported = MutableStateFlow(true)
    val isDualCameraSupported: StateFlow<Boolean> = _isDualCameraSupported

    private var layoutMode: LayoutMode = LayoutMode.PIP

    fun setLayoutMode(mode: LayoutMode) { layoutMode = mode }

    fun startCameras() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            rebindIfReady()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun rebindIfReady() {
        if (frontPreviewView == null && backPreviewView == null) return
        val p = cameraProvider ?: return
        bindCameras(p)
    }

    private fun bindCameras(provider: ProcessCameraProvider) {
        provider.unbindAll()
        val rotation = getDisplayCompat()?.rotation ?: 0
        val frontSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        val backSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val frontPreview = Preview.Builder().setTargetRotation(rotation).build()
        val backPreview = Preview.Builder().setTargetRotation(rotation).build()
        frontPreview.setSurfaceProvider(frontPreviewView?.surfaceProvider)
        backPreview.setSurfaceProvider(backPreviewView?.surfaceProvider)

        try {
            // Both cameras must be bound through CameraX concurrent mode.
            provider.bindToLifecycle(listOf(
                ConcurrentCamera.SingleCameraConfig(
                    backSelector,
                    UseCaseGroup.Builder().addUseCase(backPreview).build(),
                    lifecycleOwner
                ),
                ConcurrentCamera.SingleCameraConfig(
                    frontSelector,
                    UseCaseGroup.Builder().addUseCase(frontPreview).build(),
                    lifecycleOwner
                )
            ))
            _isDualCameraSupported.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Concurrent camera bind failed", e)
            _isDualCameraSupported.value = false
            try {
                provider.bindToLifecycle(lifecycleOwner, backSelector, backPreview)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Back camera fallback failed", fallbackError)
            }
        }
    }
    private fun getDisplayCompat(): Display? {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        return manager?.getDisplay(0)
    }

    fun release() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }
}
