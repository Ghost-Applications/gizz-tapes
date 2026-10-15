package gizz.tapes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import gizz.tapes.lastfm.LastFmAuthManager
import gizz.tapes.ui.components.CastButton
import platform.UIKit.UIViewController

// Used in iOS code
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    val appGraph = createGraphFactory<IosAppGraph.Factory>().create(AppContext())
    currentAppGraph = appGraph
    appGraph.scrobbler.start()
    return ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        }
    ) {
        CompositionLocalProvider(
            LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
            LocalPlatformActions provides { CastButton() },
        ) {
            GizzTapesApp()
        }
    }.also { RootViewControllerHolder.current = it }
}

private var currentAppGraph: IosAppGraph? = null

// Used in iOS code, from SwiftUI's onOpenURL
@Suppress("unused")
fun handleOpenUrl(url: String) {
    if (url.startsWith(LastFmAuthManager.CALLBACK_URL)) {
        currentAppGraph?.lastFmAuthManager?.onCallback()
    }
}
