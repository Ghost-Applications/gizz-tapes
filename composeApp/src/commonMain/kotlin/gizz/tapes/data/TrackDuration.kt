package gizz.tapes.data

import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@JvmInline
value class TrackDuration(val duration: Duration) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration.inWholeSeconds
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
}
