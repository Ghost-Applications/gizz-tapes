package gizz.tapes.ui.show

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@JvmInline
value class TrackDuration(val duration: Duration) {
    val formatedDuration: String get() {
        val seconds = duration.inWholeSeconds - duration.inWholeMinutes.minutes.inWholeSeconds
        val secondsString = seconds.toString().takeIf { it.length > 1 } ?: "0$seconds"

        return "${duration.inWholeMinutes}:$secondsString"
    }
}
