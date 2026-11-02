package gizz.tapes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import gizz.tapes.nav.DeepLinkHandler
import gizz.tapes.ui.components.CastButton

@Inject
@ActivityKey
@ContributesIntoMap(AppScope::class, binding<Activity>())
class MainActivity(
    private val metroViewModelFactory: MetroViewModelFactory,
    private val deepLinkHandler: DeepLinkHandler,
) : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // on recreation the link was already handled before the config change
        if (savedInstanceState == null) intent.dataString?.let(deepLinkHandler::handle)
        setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides metroViewModelFactory,
                LocalPlatformActions provides { CastButton() }
            ) {
                GizzTapesApp(deepLinkHandler)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let(deepLinkHandler::handle)
    }
}
