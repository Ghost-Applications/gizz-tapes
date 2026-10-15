package gizz.tapes.lastfm

import androidx.datastore.core.DataStore
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.playback.GizzMediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Watches playback and reports it to Last.fm when the user is logged in. Scrobbles are queued on disk
 * and sent oldest first in batches, so plays made offline are sent once the network is back.
 */
@Inject
@SingleIn(AppScope::class)
class Scrobbler(
    private val player: GizzMediaPlayer,
    private val api: LastFmApiClient,
    private val auth: LastFmAuthManager,
    private val queue: DataStore<ScrobbleQueue>,
) {
    private val logger = Logger.withTag("Scrobbler")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracker = ScrobbleTracker()
    private val flushMutex = Mutex()

    private var started = false
    private var retryJob: Job? = null
    private var retryDelay = INITIAL_RETRY_DELAY

    fun start() {
        if (started) return
        started = true

        scope.launch {
            player.state.collect { state ->
                val event = tracker.onState(state) ?: return@collect
                val session = auth.session.value ?: return@collect
                when (event) {
                    is ScrobbleTracker.Event.NowPlaying -> launch { nowPlaying(session, event.track) }
                    is ScrobbleTracker.Event.Scrobble -> {
                        queue.updateData { it.copy(items = it.items + event.scrobble) }
                        launch { flush() }
                    }
                }
            }
        }

        // Sends anything left over from a previous run, and anything queued before logging in.
        scope.launch {
            auth.session.filterNotNull().collect { flush() }
        }
    }

    private suspend fun nowPlaying(session: LastFmSession, track: LastFmTrack) {
        // Optional per Last.fm and not worth retrying.
        api.updateNowPlaying(session.key, track).onLeft { error ->
            logger.w(error) { "updateNowPlaying failed" }
            if (error is LastFmError && error.isInvalidSession) auth.logout()
        }
    }

    internal suspend fun flush() = flushMutex.withLock {
        while (true) {
            val session = auth.session.value ?: return@withLock
            val batch = queue.data.first().items.take(LastFmApiClient.MAX_SCROBBLES_PER_REQUEST)
            if (batch.isEmpty()) return@withLock

            val error = api.scrobble(session.key, batch).leftOrNull()
            when {
                error == null -> retryDelay = INITIAL_RETRY_DELAY
                error is LastFmError && error.isInvalidSession -> {
                    logger.w(error) { "Last.fm session revoked" }
                    auth.logout()
                    return@withLock
                }
                // Anything that isn't a Last.fm error is a network failure, which is worth retrying.
                error !is LastFmError || error.isRetryable -> {
                    logger.w(error) { "Scrobble failed, retrying in $retryDelay" }
                    scheduleRetry()
                    return@withLock
                }
                else -> logger.e(error) { "Scrobble rejected, dropping ${batch.size} scrobbles" }
            }
            // Only this function removes items and new ones are appended, so the batch is still at the front.
            queue.updateData { it.copy(items = it.items.drop(batch.size)) }
        }
    }

    private fun scheduleRetry() {
        if (retryJob?.isActive == true) return
        val wait = retryDelay
        retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY)
        retryJob = scope.launch {
            delay(wait)
            flush()
        }
    }

    private companion object {
        val INITIAL_RETRY_DELAY: Duration = 30.seconds
        val MAX_RETRY_DELAY: Duration = 30.minutes
    }
}
