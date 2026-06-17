package com.fydualcamera.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fydualcamera.app.camera.DualCameraManager
import com.fydualcamera.app.layout.LayoutMode
import kotlin.math.roundToInt

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dualCameraManager = remember { DualCameraManager(context, lifecycleOwner) }

    var layoutMode by remember { mutableStateOf(LayoutMode.PIP) }
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }

    // Create PreviewViews ONCE and reuse across layouts
    val backPreview = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val frontPreview = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Set preview views once
    LaunchedEffect(Unit) {
        dualCameraManager.backPreviewView = backPreview
        dualCameraManager.frontPreviewView = frontPreview
    }

    val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    var permissionsGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        permissionsGranted = granted.values.all { it }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            dualCameraManager.startCameras()
        }
    }

    DisposableEffect(Unit) {
        onDispose { dualCameraManager.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { backPreview },
                modifier = when (layoutMode) {
                    LayoutMode.PIP -> Modifier.fillMaxSize()
                    LayoutMode.LEFT_RIGHT -> Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(splitRatio)
                        .align(Alignment.CenterStart)
                    LayoutMode.TOP_BOTTOM -> Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(splitRatio)
                        .align(Alignment.TopCenter)
                    LayoutMode.FREE -> Modifier.fillMaxSize()
                }
            )

            AndroidView(
                factory = { frontPreview },
                modifier = when (layoutMode) {
                    LayoutMode.PIP -> Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                        .align(Alignment.BottomEnd)
                        .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                pipOffsetX += dragAmount.x
                                pipOffsetY += dragAmount.y
                            }
                        }
                    LayoutMode.LEFT_RIGHT -> Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f - splitRatio)
                        .align(Alignment.CenterEnd)
                    LayoutMode.TOP_BOTTOM -> Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(1f - splitRatio)
                        .align(Alignment.BottomCenter)
                    LayoutMode.FREE -> Modifier
                        .size(150.dp)
                        .align(Alignment.Center)
                }
            )

            // Split divider for LEFT_RIGHT — at the right edge of the back camera area
            if (layoutMode == LayoutMode.LEFT_RIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(splitRatio)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(Color.White)
                            .align(Alignment.CenterEnd)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    splitRatio = (splitRatio + dragAmount.x / 1000f).coerceIn(0.2f, 0.8f)
                                }
                            }
                    )
                }
            }

            // Split divider for TOP_BOTTOM — at the bottom edge of the back camera area
            if (layoutMode == LayoutMode.TOP_BOTTOM) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(splitRatio)
                        .align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.White)
                            .align(Alignment.BottomCenter)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    splitRatio = (splitRatio + dragAmount.y / 1000f).coerceIn(0.2f, 0.8f)
                                }
                            }
                    )
                }
            }

            // Layout mode labels
            if (layoutMode == LayoutMode.LEFT_RIGHT) {
                Text(
                    text = "← 拖动分割线调整比例 →",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0x88000000), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                )
            }
            if (layoutMode == LayoutMode.TOP_BOTTOM) {
                Text(
                    text = "拖动分割线调整比例",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0x88000000), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                )
            }
            if (layoutMode == LayoutMode.FREE) {
                Text(
                    text = "自由布局: 拖拽移动, 缩放调整大小",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                        .background(Color(0x88000000), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                )
            }

            // Layout mode buttons
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LayoutMode.entries.forEach { mode ->
                    LayoutModeButton(
                        mode = mode,
                        isSelected = layoutMode == mode,
                        onClick = { layoutMode = mode }
                    )
                }
            }
        }


    }
}

@Composable
private fun LayoutModeButton(mode: LayoutMode, isSelected: Boolean, onClick: () -> Unit) {
    val label = when (mode) {
        LayoutMode.PIP -> "PIP"
        LayoutMode.LEFT_RIGHT -> "左右"
        LayoutMode.TOP_BOTTOM -> "上下"
        LayoutMode.FREE -> "自由"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x88000000),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
