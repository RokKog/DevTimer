package com.rokkogovsek.devtimer

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DevTimer",
        resizable = true
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(700, 500)
        }

        MaterialTheme {
            TimerApp()
        }
    }
}
