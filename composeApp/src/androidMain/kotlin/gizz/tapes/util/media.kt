package gizz.tapes.util

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import gizz.tapes.data.BAND_NAME
import gizz.tapes.nav.Destination
import gizz.tapes.playback.MediaId
import gizz.tapes.playback.PlaybackItem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

val MediaItem?.title: String get() = this?.mediaMetadata?.title?.toString() ?: "--"
val MediaItem.showExtras: Destination.Show? get() = mediaMetadata.extras?.toShowInfo()
fun MediaItem.Builder.setMediaId(mediaId: MediaId): MediaItem.Builder {
    return setMediaId(mediaId.id)
}

val MediaItem.realMediaId: MediaId get() = MediaId.fromString(mediaId)

fun MediaItem.toReadableString() = """
    mediaId=${this.mediaId}
    localConfiguration=${this.localConfiguration}
    title=${this.mediaMetadata.title}
""".trimIndent()

@JvmInline
value class MediaItemWrapper(private val mediaItem: MediaItem) {
    override fun toString(): String = mediaItem.toReadableString()
}

@JvmInline
value class MediaItemsWrapper(private val mediaItems: List<MediaItem>) {
    override fun toString(): String = mediaItems.joinToString { it.toReadableString() }
}

@JvmInline
value class MediaMetaDataWrapper(private val mediaMetadata: MediaMetadata) {
    override fun toString(): String {
        return with(mediaMetadata) {
            "title: $title, albumTitle: $albumTitle, albumArtist: $albumArtist, displayTitle: $displayTitle"
        }
    }
}

fun PlaybackItem.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(url)
        .setMediaId(id)
        .setMimeType(MimeTypes.AUDIO_MPEG)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setArtist(BAND_NAME)
                .setAlbumArtist(BAND_NAME)
                .setAlbumTitle(albumTitle)
                .setTitle(title)
                .setRecordingYear(showDate.year)
                .setRecordingMonth(showDate.month.number)
                .setRecordingDay(showDate.day)
                .setArtworkUri(artworkUrl?.toUri())
                .setDurationMs(durationMs)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(Destination.Show(showId, showTitle).toExtrasBundle())
                .build()
        )
        .build()
}

fun Collection<PlaybackItem>.toMediaItems(): List<MediaItem> = map { it.toMediaItem() }

fun MediaItem.toPlaybackItem(): PlaybackItem {
    val metadata = mediaMetadata
    val show = checkNotNull(showExtras) { "MediaItem missing show extras: $mediaId" }
    return PlaybackItem(
        id = mediaId,
        url = checkNotNull(localConfiguration?.uri?.toString()) { "MediaItem missing URI: $mediaId" },
        title = metadata.title?.toString() ?: "--",
        albumTitle = metadata.albumTitle?.toString() ?: "--",
        artworkUrl = metadata.artworkUri?.toString(),
        showId = show.id,
        showTitle = show.title,
        durationMs = metadata.durationMs ?: 0L,
        showDate = LocalDate(
            year = checkNotNull(metadata.recordingYear) { "MediaItem missing recordingYear: $mediaId" },
            month = checkNotNull(metadata.recordingMonth) { "MediaItem missing recordingMonth: $mediaId" },
            day = checkNotNull(metadata.recordingDay) { "MediaItem missing recordingDay: $mediaId" },
        )
    )
}

fun Collection<MediaItem>.toPlaybackItems(): List<PlaybackItem> = map { it.toPlaybackItem() }
