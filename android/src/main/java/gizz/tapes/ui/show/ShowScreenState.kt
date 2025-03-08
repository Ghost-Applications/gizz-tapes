package gizz.tapes.ui.show

import arrow.core.NonEmptyList
import gizz.tapes.data.MediaId.TrackId
import gizz.tapes.data.PosterUrl

data class ShowScreenState(
    val removeOldMediaItemsAndAddNew: () -> Unit,
    val showPosterUrl: PosterUrl,
    val tracks: NonEmptyList<Track>,
    val recordingData: RecordingData,
) {
    data class Track(
        val id: TrackId,
        val title: TrackTitle,
        val duration: TrackDuration
    )
}
