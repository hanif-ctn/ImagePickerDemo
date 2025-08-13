package org.hanif.imagepickerdemo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Image Picker Demo ",
    ) {
        App()
    }
}