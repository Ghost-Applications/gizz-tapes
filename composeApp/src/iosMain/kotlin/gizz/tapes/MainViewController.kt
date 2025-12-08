package gizz.tapes

import androidx.compose.ui.window.ComposeUIViewController
import dev.zacsweers.metro.createGraph
import platform.UIKit.UIViewController

// Used in iOS code
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
    val appGraph = createGraph<AppGraph>()
    GizzTapesApp(appGraph.metroViewModelFactory)
}
