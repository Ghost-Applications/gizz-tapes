package gizz.tapes.ui.player

import gizz.tapes.data.BAND_NAME
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
        val artistName: String

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
                artistName: String = BAND_NAME,
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
                    artistName = artistName,
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
                    artistName = artistName,
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
                    artistName = artistName,
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
                artistName = artistName,
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
                artistName = artistName,
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
            override val artistName: String = BAND_NAME,
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
            override val artistName: String = BAND_NAME,
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
            override val artistName: String = BAND_NAME,
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
            override val artistName: String = BAND_NAME,
        ) : MediaLoaded {
            override val isPlaying = false
        }
    }
}
