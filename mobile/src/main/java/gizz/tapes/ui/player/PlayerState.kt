package gizz.tapes.ui.player

import android.net.Uri
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title

@JvmInline
value class PlayerError(val message: String)

sealed interface PlayerState {

    data object NoMedia: PlayerState

    sealed interface MediaLoaded: PlayerState {
        val isPlaying: Boolean
        val showId: ShowId
        val showTitle: FullShowTitle
        val durationInfo: MediaDurationInfo
        val artworkUri: Uri?
        val title: String
        val albumTitle: String
        val mediaId: String

        companion object {
            operator fun invoke(
                isPlaying: Boolean,
                isLoading: Boolean,
                showId: ShowId,
                showTitle: FullShowTitle,
                durationInfo: MediaDurationInfo,
                artworkUri: Uri?,
                title: String,
                albumTitle: String,
                mediaId: String,
            ): MediaLoaded = when {
                isPlaying -> Playing(
                    showId = showId,
                    showTitle = showTitle,
                    durationInfo = durationInfo,
                    artworkUri = artworkUri,
                    title = title,
                    albumTitle = albumTitle,
                    mediaId = mediaId
                )

                isLoading -> Loading(
                    showId = showId,
                    showTitle = showTitle,
                    durationInfo = durationInfo,
                    artworkUri = artworkUri,
                    title = title,
                    albumTitle = albumTitle,
                    mediaId = mediaId
                )

                else -> Paused(
                    showId = showId,
                    showTitle = showTitle,
                    durationInfo = durationInfo,
                    artworkUri = artworkUri,
                    title = title,
                    albumTitle = albumTitle,
                    mediaId = mediaId
                )
            }
        }

        fun copy(isPlaying: Boolean) = when (isPlaying) {
            true -> Playing(
                showId = showId,
                showTitle = showTitle,
                durationInfo = durationInfo,
                artworkUri = artworkUri,
                title = title,
                albumTitle = albumTitle,
                mediaId = mediaId
            )

            false -> Paused(
                showId = showId,
                showTitle = showTitle,
                durationInfo = durationInfo,
                artworkUri = artworkUri,
                title = title,
                albumTitle = albumTitle,
                mediaId = mediaId
            )
        }

        data class Playing(
            override val showId: ShowId,
            override val showTitle: FullShowTitle,
            override val durationInfo: MediaDurationInfo,
            override val artworkUri: Uri?,
            override val title: String,
            override val albumTitle: String,
            override val mediaId: String
        ) : MediaLoaded {
            override val isPlaying = true
        }

        data class Paused(
            override val showId: ShowId,
            override val showTitle: FullShowTitle,
            override val durationInfo: MediaDurationInfo,
            override val artworkUri: Uri?,
            override val title: String,
            override val albumTitle: String,
            override val mediaId: String
        ): MediaLoaded {
            override val isPlaying = false
        }

        data class Loading(
            override val showId: ShowId,
            override val showTitle: FullShowTitle,
            override val durationInfo: MediaDurationInfo,
            override val artworkUri: Uri?,
            override val title: String,
            override val albumTitle: String,
            override val mediaId: String
        ): MediaLoaded {
            override val isPlaying = false
        }

        data class Error(
            val playerError: PlayerError,
            override val showId: ShowId,
            override val showTitle: FullShowTitle,
            override val durationInfo: MediaDurationInfo,
            override val artworkUri: Uri?,
            override val title: String,
            override val albumTitle: String,
            override val mediaId: String
        ): MediaLoaded {
            override val isPlaying = false
        }
    }
}
