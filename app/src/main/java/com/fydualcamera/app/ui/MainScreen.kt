package com.fydualcamera.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class Screen(val label: String) {
    CAMERA("Camera"),
    GALLERY("Gallery")
}

@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf(Screen.CAMERA) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.CAMERA,
                    onClick = { currentScreen = Screen.CAMERA },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    label = { Text(Screen.CAMERA.label) }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.GALLERY,
                    onClick = { currentScreen = Screen.GALLERY },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    label = { Text(Screen.GALLERY.label) }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentScreen) {
                Screen.CAMERA -> CameraScreen()
                Screen.GALLERY -> GalleryScreen()
            }
        }
    }
}