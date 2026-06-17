package com.fydualcamera.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxHeight
import com.fydualcamera.app.layout.LayoutMode
import kotlin.math.roundToInt

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var layoutMode by remember { mutableStateOf(LayoutMode.PIP) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }

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
            when (layoutMode) {
                LayoutMode.PIP -> PipLayout(
                    pipOffsetX = pipOffsetX,
                    pipOffsetY = pipOffsetY,
                    onPipDrag = { dx, dy ->
                        pipOffsetX += dx
                        pipOffsetY += dy
                    }
                )
                LayoutMode.LEFT_RIGHT -> LeftRightLayout(splitRatio = splitRatio, onSplitChange = { splitRatio = it })
                LayoutMode.TOP_BOTTOM -> TopBottomLayout(splitRatio = splitRatio, onSplitChange = { splitRatio = it })
                LayoutMode.FREE -> FreeLayout()
            }

            // Layout mode selector
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
            IconButton(onClick = { /* toggle flash */ }) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            if (isRecording) {
                if (isPaused) {
                    FloatingActionButton(
                        onClick = { isPaused = false },
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
                        onClick = { isPaused = true },
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
                    onClick = { isRecording = false; isPaused = false },
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
                    onClick = { isRecording = true },
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

            IconButton(onClick = { /* take photo */ }) {
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

@Composable
private fun PipLayout(
    pipOffsetX: Float,
    pipOffsetY: Float,
    onPipDrag: (Float, Float) -> Unit
) {
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewView(
            modifier = Modifier.fillMaxSize(),
            isFront = false
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        pipOffsetX.roundToInt(),
                        pipOffsetY.roundToInt()
                    )
                }
                .size(120.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPipDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            CameraPreviewView(
                modifier = Modifier.fillMaxSize(),
                isFront = true
            )
        }
    }
}

@Composable
private fun LeftRightLayout(splitRatio: Float, onSplitChange: (Float) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            CameraPreviewView(
                modifier = Modifier
                    .weight(splitRatio)
                    .fillMaxHeight(),
                isFront = false
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newRatio = (splitRatio + dragAmount.x / 1000f)
                                .coerceIn(0.2f, 0.8f)
                            onSplitChange(newRatio)
                        }
                    }
            )

            CameraPreviewView(
                modifier = Modifier
                    .weight(1f - splitRatio)
                    .fillMaxHeight(),
                isFront = true
            )
        }

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
}

@Composable
private fun TopBottomLayout(splitRatio: Float, onSplitChange: (Float) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CameraPreviewView(
                modifier = Modifier
                    .weight(splitRatio)
                    .fillMaxWidth(),
                isFront = false
            )

            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth()
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newRatio = (splitRatio + dragAmount.y / 1000f)
                                .coerceIn(0.2f, 0.8f)
                            onSplitChange(newRatio)
                        }
                    }
            )

            CameraPreviewView(
                modifier = Modifier
                    .weight(1f - splitRatio)
                    .fillMaxWidth(),
                isFront = true
            )
        }

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
}

@Composable
private fun FreeLayout() {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewView(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            isFront = false
        )

        CameraPreviewView(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                .align(Alignment.Center),
            isFront = true
        )

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
}

@Composable
private fun CameraPreviewView(
    modifier: Modifier,
    isFront: Boolean
) {
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier
    )
}
