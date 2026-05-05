package gizz.tapes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

fun main() = application {
    val appGraph = createGraphFactory<DesktopAppGraph.Factory>().create(AppContext())
    val state = rememberWindowState(width = 1200.dp, height = 800.dp)

    Window(
        state = state,
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
