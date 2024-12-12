package gizz.tapes.playback

import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import arrow.core.Either
import arrow.core.getOrElse
import arrow.resilience.Schedule
import arrow.resilience.retryEither
import com.google.common.collect.ImmutableList
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.BandName
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Settings
import gizz.tapes.data.Year
import gizz.tapes.util.showTitle
import gizz.tapes.util.title
import gizz.tapes.util.toAlbumFormat
import gizz.tapes.util.tryAndGetPreferredRecordingType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MediaItemTree @Inject constructor(
    private val apiClient: GizzTapesApiClient,
    private val dataStore: DataStore<Settings>,
) {
    private data class MediaItemNode(
        val item: MediaItem,
        val children: MutableList<MediaItemNode> = mutableListOf()
    ) {
        val id = item.mediaId
    }

    private val years: MutableMap<String, MediaItemNode> = mutableMapOf()
    private val shows: MutableMap<String, MediaItemNode> = mutableMapOf()
    private val tracks: MutableMap<String, MediaItemNode> = mutableMapOf()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val root = MediaItem.Builder()
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Gizz Tape Shows!")
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_YEARS)
                    .build()
            )
            .setMediaId("ROOT")
            .build()

    fun getRoot(): MediaItem {
        return root
    }

    @UnstableApi
    suspend fun getChildren(parentId: String): ImmutableList<MediaItem> {
        Timber.d("getChildren() parentId=%s", parentId)
        if (root.mediaId == parentId) {
            val years: List<MediaItemNode> = retryForever { apiClient.shows() }
                .groupBy { it.date }
                .map { (key, value) ->
                    Year(key.year) to PosterUrl(value.random().posterUrl)
                }
                .reversed()
                .map { (year, posterUrl) ->
                    MediaItemNode(
                        MediaItem.Builder()
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(year.value)
                                    .setIsPlayable(false)
                                    .setIsBrowsable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                                    .setArtworkUri(posterUrl.toUri())
                                    .build()
                            )
                            .setMediaId(year.value)
                            .build()
                    )
                }

            years.forEach {
                this@MediaItemTree.years[it.id] = it
            }

            return ImmutableList.copyOf(years.map { it.item })
        }

        // years
        val year = years[parentId]

        if (year != null) {
            if (year.children.isEmpty()) {
                // get shows for year add to the
                val shows = retryForever { apiClient.shows() }
                    .filter { it.date.year.toString() == year.id }
                    .reversed()
                    .map {
                        MediaItem.Builder()
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(it.showTitle)
                                    .setDisplayTitle("${it.date.toAlbumFormat()} ${it.showTitle}")
                                    .setReleaseYear(it.date.year)
                                    .setReleaseDay(it.date.dayOfMonth)
                                    .setReleaseMonth(it.date.monthNumber)
                                    .setIsPlayable(true)
                                    .setIsBrowsable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                                    .setArtworkUri(PosterUrl(it.posterUrl).toUri())
                                    .build()
                            )
                            .setMediaId(it.id)
                            .build()
                    }.map { MediaItemNode(item = it) }

                shows.forEach {
                    this@MediaItemTree.shows[it.id] = it
                }
                year.children.addAll(shows)
            }

            return ImmutableList.copyOf(year.children.map { it.item })
        }

        val show = shows[parentId]

        if (show != null) {
            if (show.children.isEmpty()) {
                val showData = retryForever { apiClient.show(show.id) }

                val showMetadata = show.item.mediaMetadata
                val date =
                    "${showMetadata.releaseYear}/${showMetadata.releaseMonth}/${showMetadata.releaseDay}"

                val preferredRecordingType =
                    dataStore.data.map { it.preferredRecordingType }.first()
                val recording =
                    showData.recordings.tryAndGetPreferredRecordingType(preferredRecordingType)

                val showChildren = recording.files.map { track ->
                    MediaItem.Builder()
                        .setUri(recording.filesPathPrefix + track.filename)
                        .setMediaId(track.filename)
                        .setMimeType(MimeTypes.AUDIO_MPEG)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setExtras(
                                    Bundle().apply {
                                        putString("showId", show.id)
                                        putString("showTitle", show.item.title)
                                    }
                                )
                                .setArtist("$date ${show.item.title}")
                                .setAlbumArtist(BandName)
                                .setAlbumTitle(show.item.title)
                                .setTitle(track.title)
                                .setRecordingYear(showData.date.year)
                                .setArtworkUri(PosterUrl(showData.posterUrl).toUri())
                                .setDurationMs(track.length.inWholeMilliseconds)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .setIsPlayable(true)
                                .setIsBrowsable(false)
                                .build()
                        )
                        .build()
                }.map { mi -> MediaItemNode(mi) }

                showChildren.forEach {
                    tracks[it.id] = it
                }
                show.children.addAll(showChildren)
            }

            return ImmutableList.copyOf(show.children.map { c -> c.item })
        }

        Timber.w("No children for parentId=%s", parentId)
        return ImmutableList.of()
    }

    fun getItem(mediaId: String): MediaItem {
        if (root.mediaId == mediaId) {
            return root
        }

        val year = years[mediaId]
        if (year != null) {
            return year.item
        }

        val show = shows[mediaId]
        if (show != null) {
            return show.item
        }

        val track = tracks[mediaId]
        if (track != null) {
            return track.item
        }

        error("Unknown mediaId=$mediaId")
    }

    /**
     * Retries the action every 100 milliseconds up to 3 seconds and then
     * continues to retry again forever every 3 seconds
     */
    private suspend inline fun <Result> retryForever(action: () -> Either<Throwable, Result>): Result {
        return Schedule.exponential<Throwable>(100.milliseconds)
            .doWhile { _, duration -> duration < 3.seconds }
            .andThen(Schedule.spaced(3.seconds))
            .retryEither(action)
            .getOrElse { error("This shouldn't happen") }
    }
}
