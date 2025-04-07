package gizz.tapes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Gizz Tapes KMP",
    ) {
        App()
    }
}