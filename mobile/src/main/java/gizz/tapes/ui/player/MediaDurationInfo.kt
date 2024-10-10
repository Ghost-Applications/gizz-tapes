package gizz.tapes.ui.player

import gizz.tapes.util.formatedElapsedTime

data class MediaDurationInfo(
    val currentPosition: Long,
    val duration: Long
) {
    val currentPositionFloat = currentPosition.toFloat()

    val elapsedTimeString by lazy { currentPosition.formatedElapsedTime }
    val durationTimeString by lazy { duration.formatedElapsedTime }
}
