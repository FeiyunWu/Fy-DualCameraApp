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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.fydualcamera.app.db.AppDatabase
import com.fydualcamera.app.layout.LayoutMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dualCameraManager = remember { DualCameraManager(context, lifecycleOwner) }
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val mediaDao = remember { db.mediaDao() }

    var layoutMode by remember { mutableStateOf(LayoutMode.PIP) }
    var isPaused by remember { mutableStateOf(false) }
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }

    val isRecording by dualCameraManager.isRecording.collectAsState()

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

    // Handle media saved -> insert into DB
    LaunchedEffect(Unit) {
        dualCameraManager.onMediaSaved = { entity ->
            scope.launch { mediaDao.insert(entity) }
        }
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
                        .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                        .align(Alignment.BottomEnd)
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

            // Split divider for LEFT_RIGHT
            if (layoutMode == LayoutMode.LEFT_RIGHT) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                        .align(Alignment.CenterStart)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                splitRatio = (splitRatio + dragAmount.x / 1000f).coerceIn(0.2f, 0.8f)
                            }
                        }
                )
            }

            // Split divider for TOP_BOTTOM
            if (layoutMode == LayoutMode.TOP_BOTTOM) {
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .fillMaxWidth()
                        .background(Color.White)
                        .align(Alignment.TopCenter)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                splitRatio = (splitRatio + dragAmount.y / 1000f).coerceIn(0.2f, 0.8f)
                            }
                        }
                )
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

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            if (isRecording) {
                if (isPaused) {
                    FloatingActionButton(
                        onClick = {
                            isPaused = false
                            dualCameraManager.startRecording()
                        },
                        containerColor = Color(0xFFFF9800),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    FloatingActionButton(
                        onClick = {
                            isPaused = true
                            dualCameraManager.stopRecording()
                        },
                        containerColor = Color(0xFFF44336),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        isPaused = false
                        dualCameraManager.stopRecording()
                    },
                    containerColor = Color(0xFF757575),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = { dualCameraManager.startRecording() },
                    containerColor = Color(0xFFF44336),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            IconButton(onClick = { dualCameraManager.takePhoto() }) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White
                )
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
