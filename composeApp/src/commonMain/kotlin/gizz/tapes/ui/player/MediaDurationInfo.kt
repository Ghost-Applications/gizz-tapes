package gizz.tapes.ui.player

data class MediaDurationInfo(
    val currentPosition: Long,
    val duration: Long
) {
    val currentPositionFloat: Float = if (duration > 0) currentPosition.toFloat() / duration else 0f

    val elapsedTimeString: String get() = formatTime(currentPosition)
    val durationTimeString: String get() = formatTime(duration)

    companion object {
        val Empty = MediaDurationInfo(0L, 0L)

        private fun formatTime(ms: Long): String {
            val totalSeconds = maxOf(ms, 0L) / 1000L
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
    }
}
