package gizz.tapes.playback

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import arrow.fx.coroutines.parMap
import gizz.tapes.data.BandName
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.ui.nav.Show
import gizz.tapes.util.MediaItemWrapper
import gizz.tapes.util.showExtras
import gizz.tapes.util.toExtrasBundle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class StoredMediaSession(
    val currentTrack: Int? = null,
    val currentTime: Long? = null,
    val mediaItems: List<MediaStorageItem> = emptyList()
)

@Serializable
data class MediaStorageItem(
    val uri: String,
    val mediaId: String,
    val showId: String,
    val albumTitle: String,
    val trackTitle: String,
    val showYear: Int,
    val showMonth: Int,
    val showDay: Int,
    val artworkUrl: String,
    val trackDurationMs: Long,
)

@kotlin.OptIn(ExperimentalSerializationApi::class)
class StoredMediaSessionSerializer : Serializer<StoredMediaSession> {
    override val defaultValue: StoredMediaSession = StoredMediaSession()

    override suspend fun readFrom(input: InputStream): StoredMediaSession {
        try {
            return Json.decodeFromStream(input)
        } catch (e: Exception) {
            Timber.e(e, "Error decoding file")
            return StoredMediaSession()
        }
    }

    override suspend fun writeTo(t: StoredMediaSession, output: OutputStream) {
        try {
            Json.encodeToStream(t, output)
        } catch (e: Exception) {
            Timber.e(e, "Error saving media session")
        }
    }

}

@OptIn(UnstableApi::class)
@Singleton
class CurrentlyPlayingSaver @Inject constructor(
    private val dataStorage: DataStore<StoredMediaSession>,
) {
    suspend fun mediaItems(): List<MediaItem> {
        return dataStorage.data
            .map { it.mediaItems }
            .first()
            .parMap { it.toMediaItem() }
    }

    suspend fun currentTrack(): Int {
        return dataStorage.data
            .map { it.currentTrack }
            .first() ?: 0
    }

    suspend fun currentPosition(): Long {
        return dataStorage.data
            .map { it.currentTime }
            .first() ?: 0
    }

    suspend fun saveCurrentState(
        mediaItems: List<MediaItem>,
        currentTrackIndex: Int,
        currentPosition: Long,
    ) {
        Timber.d(
            "saveCurrentState() currentTrackIndex=%s currentPosition=%s",
            currentTrackIndex,
            currentPosition
        )
        dataStorage.updateData {
            val storageItems = mediaItems.parMap { it.toMediaStorageItem() }
                .filterNotNull()

            StoredMediaSession(
                currentTrack = currentTrackIndex,
                currentTime = currentPosition,
                mediaItems = storageItems
            )
        }
    }

    @UnstableApi
    private fun MediaItem.toMediaStorageItem(): MediaStorageItem? {
        val metaData = mediaMetadata
        val localConfig =
            checkNotNull(localConfiguration) { "localConfiguration should not be null" }
        val (showId, _) = showExtras ?: run {
            Timber.e("no extras for %s", MediaItemWrapper(this))
            return null
        }

        return MediaStorageItem(
            uri = localConfig.uri.toString(),
            mediaId = mediaId,
            showId = showId.value,
            albumTitle = metaData.albumTitle.toString(),
            trackTitle = metaData.title.toString(),
            showYear = checkNotNull(metaData.recordingYear),
            showMonth = checkNotNull(metaData.recordingMonth),
            showDay = checkNotNull(metaData.recordingDay),
            artworkUrl = metaData.artworkUri.toString(),
            trackDurationMs = checkNotNull(metaData.durationMs)
        )
    }

    private fun MediaStorageItem.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(mediaId)
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setExtras(
                        Show(
                           id = ShowId(showId),
                           title = FullShowTitle(
                               title = Title(albumTitle),
                               date = LocalDate(showYear, showMonth, showDay)
                           )
                        ).toExtrasBundle()
                    )
                    .setArtist(BandName)
                    .setAlbumArtist(BandName)
                    .setAlbumTitle(albumTitle)
                    .setTitle(trackTitle)
                    .setRecordingYear(showYear)
                    .setRecordingMonth(showMonth)
                    .setRecordingDay(showDay)
                    .setArtworkUri(artworkUrl.toUri())
                    .setDurationMs(trackDurationMs)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }
}
