package gizz.tapes.ui.show

import arrow.core.NonEmptyList
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.RecordingData
import gizz.tapes.data.TrackDuration
import gizz.tapes.data.TrackTitle

data class ShowScreenState(
    val removeOldMediaItemsAndAddNew: (startIndex: Int) -> Unit,
    val showPosterUrl: PosterUrl,
    val tracks: NonEmptyList<Track>,
    val recordingData: RecordingData,
) {
    data class Track(
        val id: String,
        val title: TrackTitle,
        val duration: TrackDuration,
    )
}
