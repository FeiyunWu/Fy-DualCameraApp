package com.fydualcamera.app.camera

enum class CameraPosition(val desc: String) {
    FRONT("前置"),
    BACK("后置");

    companion object {
        fun fromCameraLens(lens: Int): CameraPosition {
            return if (lens == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) FRONT else BACK
        }
    }
}
