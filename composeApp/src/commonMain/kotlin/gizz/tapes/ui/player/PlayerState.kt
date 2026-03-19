package gizz.tapes.ui.player

import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import kotlin.jvm.JvmInline

@JvmInline
value class PlayerError(val message: String)

sealed interface PlayerState {

    data object NoMedia : PlayerState

    sealed interface MediaLoaded : PlayerState {
        val isPlaying: Boolean
        val showId: ShowId
        val showTitle: FullShowTitle
        val durationInfo: MediaDurationInfo
        val artworkUri: String?
        val title: String
        val albumTitle: String
        val mediaId: String
        val currentTrackIndex: Int

        companion object {
            operator fun invoke(
                isPlaying: Boolean,
                isLoading: Boolean,
                showId: ShowId,
                showTitle: FullShowTitle,
                durationInfo: MediaDurationInfo,
                artworkUri: String?,
                title: String,
                albumTitle: String,
                mediaId: String,
                currentTrackIndex: Int,
            ): MediaLoaded = when {
                isPlaying -> Playing(
                    showId = showId,
                    showTitle = showTitle,
                    durationInfo = durationInfo,
                    artworkUri = artworkUri,
                    title = title,
                    albumTitle = albumTitle,
                    mediaId = mediaId,
                    currentTrackIndex = currentTrackIndex,
                )

                isLoading -> Loading(
                    showId = showId,
                    showTitle = showTitle,
                    durationInfo = durationInfo,
                    artworkUri = artworkUri,
                    title = title,
                    albumTitle = albumTitle,
                    mediaId = mediaId,
                    currentTrackIndex = currentTrackIndex,
                )

                else -> Paused(
                    showId = showId,
                    showTitle = showTitle,
                    durationInfo = durationInfo,
                    artworkUri = artworkUri,
                    title = title,
                    albumTitle = albumTitle,
                    mediaId = mediaId,
                    currentTrackIndex = currentTrackIndex,
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
                mediaId = mediaId,
                currentTrackIndex = currentTrackIndex,
            )

            false -> Paused(
                showId = showId,
                showTitle = showTitle,
                durationInfo = durationInfo,
                artworkUri = artworkUri,
                title = title,
                albumTitle = albumTitle,
                mediaId = mediaId,
                currentTrackIndex = currentTrackIndex,
            )
        }

        data class Playing(
            override val showId: ShowId,
            override val showTitle: FullShowTitle,
            override val durationInfo: MediaDurationInfo,
            override val artworkUri: String?,
            override val title: String,
            override val albumTitle: String,
            override val mediaId: String,
            override val currentTrackIndex: Int,
        ) : MediaLoaded {
            override val isPlaying = true
        }

        data class Paused(
            override val showId: ShowId,
            override val showTitle: FullShowTitle,
            override val durationInfo: MediaDurationInfo,
            override val artworkUri: String?,
            override val title: String,
            override val albumTitle: String,
            override val mediaId: String,
            override val currentTrackIndex: Int,
        ) : MediaLoaded {
            override val isPlaying = false
        }

        data class Loading(
            override val showId: ShowId,
            override val showTitle: FullShowTitle,
            override val durationInfo: MediaDurationInfo,
            override val artworkUri: String?,
            override val title: String,
            override val albumTitle: String,
            override val mediaId: String,
            override val currentTrackIndex: Int,
        ) : MediaLoaded {
            override val isPlaying = false
        }

        data class Error(
            val playerError: PlayerError,
            override val showId: ShowId,
            override val showTitle: FullShowTitle,
            override val durationInfo: MediaDurationInfo,
            override val artworkUri: String?,
            override val title: String,
            override val albumTitle: String,
            override val mediaId: String,
            override val currentTrackIndex: Int,
        ) : MediaLoaded {
            override val isPlaying = false
        }
    }
}
