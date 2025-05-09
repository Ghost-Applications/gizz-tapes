package gizz.tapes

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// Used in iOS code
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
    GizzTapesApp()
}
