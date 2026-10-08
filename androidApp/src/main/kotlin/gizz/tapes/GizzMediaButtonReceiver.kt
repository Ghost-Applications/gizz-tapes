package gizz.tapes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.BroadcastReceiverKey
import gizz.tapes.playback.CurrentlyPlayingSaver
import kotlinx.coroutines.runBlocking

/**
 * Skips starting [PlaybackService] for a media button press when there is no stored session to
 * resume. Otherwise the service is started in the foreground, never starts playback, and the
 * system crashes it with a ForegroundServiceDidNotStartInTimeException.
 */
@Inject
@UnstableApi
@BroadcastReceiverKey
@ContributesIntoMap(AppScope::class, binding<BroadcastReceiver>())
class GizzMediaButtonReceiver(
    private val currentlyPlayingSaver: CurrentlyPlayingSaver,
) : MediaButtonReceiver() {

    private val logger = Logger.withTag("GizzMediaButtonReceiver")

    override fun shouldStartForegroundService(context: Context, intent: Intent): Boolean {
        val hasSession = runBlocking { currentlyPlayingSaver.storedSession().items.isNotEmpty() }
        logger.d { "shouldStartForegroundService() hasSession=$hasSession" }
        return hasSession
    }
}
