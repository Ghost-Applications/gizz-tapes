package gizz.tapes.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import gizz.tapes.util.CastAvailabilityChecker

@Composable
fun CastButton() {
    if (CastAvailabilityChecker.isAvailable) {
        AndroidView(
            factory = { context ->
                MediaRouteButton(context).apply {
                    CastButtonFactory.setUpMediaRouteButton(context, this)
                }
            },
            modifier = Modifier.size(48.dp)
        )
    }
}