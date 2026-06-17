package com.fydualcamera.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
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
    CAMERA("拍摄"),
    GALLERY("作品库"),
    SETTINGS("设置")
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
                    label = { Text("拍摄") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.GALLERY,
                    onClick = { currentScreen = Screen.GALLERY },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    label = { Text("作品库") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.SETTINGS,
                    onClick = { currentScreen = Screen.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("设置") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.CAMERA -> CameraScreen()
                Screen.GALLERY -> GalleryScreen()
                Screen.SETTINGS -> SettingsScreen()
            }
        }
    }
}
