package gizz.tapes

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import gizz.tapes.playback.GizzMediaPlayer
import gizz.tapes.ui.components.CastButton

@Inject
@ActivityKey(MainActivity::class)
@ContributesIntoMap(AppScope::class, binding<Activity>())
class MainActivity(
    private val metroViewModelFactory: MetroViewModelFactory,
    private val mediaPlayer: GizzMediaPlayer,
) : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides metroViewModelFactory,
                LocalPlatformActions provides { CastButton() }
            ) {
                GizzTapesApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
