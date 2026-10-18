package gizz.tapes.playback

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Carries errors media3 can't surface itself to [AndroidMediaPlayer] - e.g. a Cast receiver
 * failing to load a track, which RemoteCastPlayer never reports as a player error.
 */
@Inject
@SingleIn(AppScope::class)
class PlaybackErrorReporter {
    private val _errors = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    fun report(message: String) {
        _errors.tryEmit(message)
    }
}
