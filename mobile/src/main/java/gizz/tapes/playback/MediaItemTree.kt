package gizz.tapes.playback

import android.os.Bundle
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
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Year
import gizz.tapes.util.bestRecording
import gizz.tapes.util.showTitle
import gizz.tapes.util.title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MediaItemTree @Inject constructor(
    private val apiClient: GizzTapesApiClient
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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val root = MediaItemNode(
        MediaItem.Builder()
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
    )

    private lateinit var mediaTree: Deferred<MediaItemNode>

    init {
        scope.launch {
            mediaTree = async {
                val years: List<Pair<Year, PosterUrl>> = retryForever { apiClient.shows() }
                    .groupBy { it.date }
                    .map { (key, value) ->
                        Year(key.year) to PosterUrl(value.random().posterUrl)
                    }
                val children = years.map { (year, posterUrl) ->
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
                root.children.addAll(children)
                children.forEach {
                    this@MediaItemTree.years[it.id] = it
                }
                root
            }
        }
    }

    suspend fun getRoot(): MediaItem {
        return mediaTree.await().item
    }

    @UnstableApi
    suspend fun getChildren(parentId: String): ImmutableList<MediaItem> {
        Timber.d("getChildren() parentId=%s", parentId)
        if (root.item.mediaId == parentId) {
            return ImmutableList.copyOf(root.children.map { it.item })
        }

        // years
        val year = years[parentId]

        if (year != null) {
            if (year.children.isEmpty()) {
                // get shows for year add to the
                val shows = retryForever { apiClient.shows() }
                    .filter { it.date.year.toString() == year.id }
                    .map {
                        MediaItem.Builder()
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(it.showTitle)
                                    .setIsPlayable(true)
                                    .setIsBrowsable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                                    .setArtworkUri(PosterUrl(it.posterUrl)?.toUri())
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
                val recording = showData.recordings.bestRecording

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
                                .setArtist("King Gizzard & The Lizard Wizard")
                                .setAlbumArtist("King Gizzard & The Lizard Wizard")
                                .setAlbumTitle(show.item.title)
                                .setTitle(track.title)
                                .setRecordingYear(showData.date.year)
                                .setArtworkUri(PosterUrl(showData.posterUrl)?.toUri())
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

    suspend fun getItem(mediaId: String): MediaItem? {
        if (root.id == mediaId) {
            return root.item
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