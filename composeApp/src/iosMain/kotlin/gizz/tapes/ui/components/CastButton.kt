package gizz.tapes.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import cocoapods.google_cast_sdk.GCKUICastButton
import gizz.tapes.playback.GizzCastContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor

// GCKUICastButton self-manages its own icon state (idle/connecting/connected) and opens Cast's
// device picker on tap - no CastButtonFactory-style setup call needed beyond GCKCastContext
// already being initialized, which AppDelegate guarantees happens before any UI is shown.
//
// Unlike Android's MediaRouteButton, GCKUICastButton doesn't hide itself when no Cast devices are
// discoverable, so this composable observes GizzCastContext.isCastAvailable to do that itself.
@OptIn(ExperimentalForeignApi::class)
@Composable
fun CastButton() {
    val isAvailable by GizzCastContext.isCastAvailable.collectAsState()
    if (!isAvailable) return

    UIKitView(
        factory = {
            GCKUICastButton(frame = CGRectMake(0.0, 0.0, 24.0, 24.0)).apply {
                opaque = false
                clearBackground()
            }
        },
        modifier = Modifier.size(48.dp),
        // GCKUICastButton appears to reset its own backgroundColor internally whenever it
        // updates its icon (e.g. right after construction, once it queries the current cast
        // state), so clearing it once in `factory` loses that race - `update` re-runs on every
        // recomposition and gives us another shot to override it after the SDK's own changes.
        update = { it.clearBackground() },
        background = Color.Transparent,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun GCKUICastButton.clearBackground() {
    backgroundColor = UIColor.clearColor
    layer.backgroundColor = UIColor.clearColor.CGColor
}
