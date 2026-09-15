package gizz.tapes.util

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import gizz.tapes.data.BAND_NAME
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.nav.Destination
import gizz.tapes.playback.MediaId
import gizz.tapes.playback.PlaybackItem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.io.File

val MediaItem?.title: String get() = this?.mediaMetadata?.title?.toString() ?: "--"
val MediaItem.showExtras: Destination.Show? get() = mediaMetadata.extras?.toShowInfo()

// CastPlayer strips Bundle extras, so fall back to standard fields that survive Cast.
fun MediaItem.resolveShowDestination(): Destination.Show? {
    showExtras?.let { return it }
    val showIdStr = realMediaId.showId ?: return null
    val albumTitle = mediaMetadata.albumTitle?.toString() ?: return null
    val year = mediaMetadata.recordingYear ?: return null
    val month = mediaMetadata.recordingMonth ?: return null
    val day = mediaMetadata.recordingDay ?: return null
    return Destination.Show(
        id = ShowId(showIdStr),
        title = FullShowTitle(title = Title(albumTitle), date = LocalDate(year, month, day))
    )
}
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

// local downloads are stored as plain filesystem paths (no scheme), which media3's
// DefaultDataSource would otherwise try to fetch over the network - build a proper
// file:// Uri so it routes to FileDataSource instead.
fun String.toLocalOrRemoteUri(): Uri = if (startsWith("/")) File(this).toUri() else toUri()

fun PlaybackItem.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(url.toLocalOrRemoteUri())
        .setMediaId(id)
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
                .setExtras(Destination.Show(showId, showTitle).toExtrasBundle().putRemoteUrl(remoteUrl))
                .build()
        )
        .build()
}

fun Collection<PlaybackItem>.toMediaItems(): List<MediaItem> = map { it.toMediaItem() }

fun MediaItem.toPlaybackItem(): PlaybackItem {
    val metadata = mediaMetadata
    val date = LocalDate(
        year = checkNotNull(metadata.recordingYear) { "MediaItem missing recordingYear: $mediaId" },
        month = checkNotNull(metadata.recordingMonth) { "MediaItem missing recordingMonth: $mediaId" },
        day = checkNotNull(metadata.recordingDay) { "MediaItem missing recordingDay: $mediaId" },
    )
    // CastPlayer strips Bundle extras from MediaMetadata, so fall back to fields that survive Cast.
    val show = showExtras ?: Destination.Show(
        id = ShowId(checkNotNull(realMediaId.showId) { "MediaItem mediaId missing showId: $mediaId" }),
        title = FullShowTitle(title = Title(metadata.albumTitle?.toString() ?: "--"), date = date)
    )
    val url = checkNotNull(localConfiguration?.uri?.toString()) { "MediaItem missing URI: $mediaId" }
    return PlaybackItem(
        id = mediaId,
        url = url,
        remoteUrl = metadata.extras?.getRemoteUrl() ?: url,
        title = metadata.title?.toString() ?: "--",
        albumTitle = metadata.albumTitle?.toString() ?: "--",
        artworkUrl = metadata.artworkUri?.toString(),
        showId = show.id,
        showTitle = show.title,
        durationMs = metadata.durationMs ?: 0L,
        showDate = date
    )
}

fun Collection<MediaItem>.toPlaybackItems(): List<PlaybackItem> = map { it.toPlaybackItem() }
