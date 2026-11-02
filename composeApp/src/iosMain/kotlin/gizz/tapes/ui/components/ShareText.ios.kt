package gizz.tapes.ui.components

import androidx.compose.runtime.Composable
import gizz.tapes.RootViewControllerHolder
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberShareText(): (String) -> Unit = { text ->
    RootViewControllerHolder.current?.let { root ->
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        // iPad presents share sheets as popovers, which crash without an anchor.
        sheet.popoverPresentationController?.sourceView = root.view
        root.presentViewController(sheet, animated = true, completion = null)
    }
}
