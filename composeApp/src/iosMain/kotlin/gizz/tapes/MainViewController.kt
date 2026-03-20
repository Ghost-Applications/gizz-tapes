package gizz.tapes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import platform.UIKit.UIViewController

// Used in iOS code
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    val appGraph = createGraphFactory<IosAppGraph.Factory>().create(AppContext())
    return ComposeUIViewController {
        CompositionLocalProvider(LocalMetroViewModelFactory provides appGraph.metroViewModelFactory) {
            GizzTapesApp()
        }
    }
}
