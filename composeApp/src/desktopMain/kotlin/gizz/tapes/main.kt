package gizz.tapes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.zacsweers.metro.createGraph

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Gizz Tapes",
    ) {
        val appGraph = createGraph<AppGraph>()
        GizzTapesApp(appGraph.metroViewModelFactory)
    }
}
