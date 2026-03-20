package gizz.tapes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

fun main() = application {
    val appGraph = createGraphFactory<DesktopAppGraph.Factory>().create(AppContext())

    Window(
        onCloseRequest = {
            appGraph.mediaPlayer.release()
            exitApplication()
        },
        title = "Gizz Tapes",
    ) {
        CompositionLocalProvider(LocalMetroViewModelFactory provides appGraph.metroViewModelFactory) {
            GizzTapesApp()
        }
    }
}
