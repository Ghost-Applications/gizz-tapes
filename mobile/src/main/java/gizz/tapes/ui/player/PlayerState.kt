package gizz.tapes.ui.player

import android.net.Uri
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title

sealed interface PlayerState {

    data object NoMedia: PlayerState

    data class MediaLoaded(
        val isPlaying: Boolean,
        val showId: ShowId,
        val showTitle: Title,
        val formatedElapsedTime: String,
        val formatedDurationTime: String,
        val duration: Long,
        val currentPosition: Long,
        val artworkUri: Uri?,
        val title: String,
        val albumTitle: String,
        val mediaId: String,
    ): PlayerState
}
