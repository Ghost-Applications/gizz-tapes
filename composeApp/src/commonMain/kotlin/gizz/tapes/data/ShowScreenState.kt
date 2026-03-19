package gizz.tapes.data

import arrow.core.NonEmptyList

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
