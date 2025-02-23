package gizz.tapes.playback

import androidx.annotation.OptIn
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
import gizz.tapes.api.data.KglwFile
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Recording
import gizz.tapes.data.BandName
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.MediaId
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.data.Year
import gizz.tapes.ui.nav.Show
import gizz.tapes.util.realMediaId
import gizz.tapes.util.setMediaId
import gizz.tapes.util.showTitle
import gizz.tapes.util.title
import gizz.tapes.util.toAlbumFormat
import gizz.tapes.util.toExtrasBundle
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MediaItemTree @Inject constructor(
    private val apiClient: GizzTapesApiClient,
) {

    private data class MediaItemNode(
        val item: MediaItem,
        val children: MutableList<MediaItemNode> = mutableListOf()
    ) {
        val mediaId = item.realMediaId
    }

    private val years: MutableMap<MediaId, MediaItemNode> = mutableMapOf()
    private val shows: MutableMap<MediaId, MediaItemNode> = mutableMapOf()
    private val recordings: MutableMap<MediaId, MediaItemNode> = mutableMapOf()
    private val tracks: MutableMap<MediaId, MediaItemNode> = mutableMapOf()

    private val root = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Gizz Tape Shows!")
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_YEARS)
                .build()
        )
        .setMediaId(MediaId.RootId)
        .build()

    fun getRoot(): MediaItem {
        Timber.d("getRoot()")
        return root
    }

    @OptIn(UnstableApi::class)
    suspend fun getItem(id: MediaId): MediaItem {
        Timber.d("getItem() id=%s", id)

        // Check if the mediaId is the root
        if (root.realMediaId == id) {
            return root
        }

        // Attempt to find the media item in the existing maps
        years[id]?.let { return it.item }
        shows[id]?.let { return it.item }
        tracks[id]?.let { return it.item }

        // Attempt to load data into the media tree if mediaId is not root
        id.parent?.let { parent ->
            return getChildren(parent).firstOrNull { it.realMediaId == id }
                ?: error("Unknown mediaId=$id")
        }

        // If no parent and not found, throw an error
        error("Unknown mediaId=$id")
    }

    @UnstableApi
    suspend fun getChildren(parentId: MediaId): ImmutableList<MediaItem> {
        Timber.d("getChildren() parentId=%s", parentId)

        return when (parentId) {
            MediaId.RootId -> loadChildrenForRoot()
            is MediaId.YearId -> loadChildrenForYear(parentId)
            is MediaId.ShowId -> loadChildrenForShow(parentId)
            is MediaId.RecordingId -> loadChildrenForRecording(parentId)
            else -> {
                Timber.w("No children for parentId=%s", parentId)
                ImmutableList.of()
            }
        }
    }

    private suspend fun loadChildrenForRoot(): ImmutableList<MediaItem> {
        val years: List<MediaItemNode> = retryForever { apiClient.shows() }
            .groupBy { it.date.year }
            .map { (key, value) ->
                Year(key) to PosterUrl(value.random().posterUrl)
            }
            .reversed()
            .map { (year, posterUrl) ->
                createYearMediaItem(MediaId.YearId(year.value), posterUrl)
            }

        years.forEach {
            this@MediaItemTree.years[it.mediaId] = it
        }

        return ImmutableList.copyOf(years.map { it.item })
    }

    private suspend fun loadChildrenForYear(yearId: MediaId.YearId): ImmutableList<MediaItem> {
        val year: MediaItemNode = years[yearId] ?: run {
            loadChildrenForRoot()
            years[yearId] ?: error("something went wrong loading parents")
        }
        Timber.d("year=%s", year)

        if (year.children.isEmpty()) {
            val shows = retryForever { apiClient.shows() }
                .filter { it.date.year.toString() == year.mediaId.year }
                .reversed()
                .map { createShowMediaItem(it) }
                .map { MediaItemNode(item = it) }

            shows.forEach {
                this@MediaItemTree.shows[it.mediaId] = it
            }
            year.children.addAll(shows)
        }

        return ImmutableList.copyOf(year.children.map { it.item })
    }

    @OptIn(UnstableApi::class)
    private suspend fun loadChildrenForShow(showId: MediaId.ShowId): ImmutableList<MediaItem> {
        val show = shows[showId] ?: run {
            loadChildrenForYear(showId.parent)
            shows[showId] ?: error("Something went wrong")
        }

        if (show.children.isEmpty()) {
            val showData = retryForever { apiClient.show(checkNotNull(show.mediaId.showId)) }

            val showMetadata = show.item.mediaMetadata
            val dateString =
                "${showMetadata.releaseYear}/${showMetadata.releaseMonth}/${showMetadata.releaseDay}"

            val showChildren = showData.recordings
                .sortedBy { it.type }
                .map { recording ->
                    MediaItem.Builder()
                        .setMediaId(MediaId.RecordingId(show = showData, recording = recording))
                        .setMediaMetadata(
                            // figure out if there's a reason to show item.title
                            // instead of showData.title...
                            MediaMetadata.Builder()
                                .setTitle(showData.title)
                                .setDisplayTitle("${recording.type}: ${recording.id} ${recording.taper.orEmpty()}")
                                .setArtist("$dateString ${show.item.title}")
                                .setAlbumTitle(show.item.title)
                                .setReleaseYear(showData.date.year)
                                .setReleaseDay(showData.date.dayOfMonth)
                                .setReleaseMonth(showData.date.monthNumber)
                                .setAlbumArtist(BandName)
                                .setArtworkUri(PosterUrl(showData.posterUrl).toUri())
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                                .setIsPlayable(true)
                                .setIsBrowsable(true)
                                .build()
                        )
                        .build()
                }.map { mi -> MediaItemNode(mi) }

            showChildren.forEach {
                recordings[it.mediaId] = it
            }
            show.children.addAll(showChildren)
        }

        return ImmutableList.copyOf(show.children.map { c -> c.item })
    }

    @OptIn(UnstableApi::class)
    private suspend fun loadChildrenForRecording(recordingId: MediaId.RecordingId): ImmutableList<MediaItem> {
        Timber.d("loadChildrenForRecording() recordingId=%s", recordingId)
        val recording: MediaItemNode = recordings[recordingId] ?: run {
            loadChildrenForShow(recordingId.parent)
            recordings[recordingId] ?: error("Something went wrong")
        }
        Timber.d("loadChildrenForRecording() recordingInTree=%s", recording)

        if (recording.children.isEmpty()) {
            val showData = retryForever { apiClient.show(checkNotNull(recording.mediaId.showId)) }

            val showMetadata = recording.item.mediaMetadata
            val dateString = "${showMetadata.releaseYear}/${showMetadata.releaseMonth}/${showMetadata.releaseDay}"

            val selectedRecording = showData.recordings.first { it.id == recordingId.recordingId }

            val showChildren = selectedRecording.files.map { track ->
                createTrackMediaItem(selectedRecording, track, showData, recording, dateString)
            }.map { mi -> MediaItemNode(mi) }

            showChildren.forEach {
                tracks[it.mediaId] = it
            }
            recording.children.addAll(showChildren)
        }

        return ImmutableList.copyOf(recording.children.map { c -> c.item })
    }

    @OptIn(UnstableApi::class)
    private fun createTrackMediaItem(
        recording: Recording,
        track: KglwFile,
        showData: gizz.tapes.api.data.Show,
        show: MediaItemNode,
        dateString: String
    ): MediaItem {
        Timber.d("createTrackMediaItem() recording=%s, track=%s", recording, track)
        return MediaItem.Builder()
            .setUri(recording.filesPathPrefix + track.filename)
            .setMediaId(MediaId.TrackId(show = showData, file = track, recording = recording))
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .setMediaMetadata(
                // I think there's a reason for using `show.item.title` but I don't remember now
                // figure out when testing on a real device and leave a good comment...
                MediaMetadata.Builder()
                    .setExtras(
                        Show(
                            id = ShowId(checkNotNull(show.mediaId.showId) { "Show media id is null"}),
                            title = FullShowTitle(
                                title = Title(show.item.title),
                                date = showData.date
                            )
                        ).toExtrasBundle()
                    )
                    .setArtist("$dateString ${show.item.title}")
                    .setAlbumArtist(BandName)
                    .setAlbumTitle(show.item.title)
                    .setTitle(track.title)
                    .setRecordingYear(showData.date.year)
                    .setRecordingMonth(showData.date.monthNumber)
                    .setRecordingDay(showData.date.dayOfMonth)
                    .setArtworkUri(PosterUrl(showData.posterUrl).toUri())
                    .setDurationMs(track.length.inWholeMilliseconds)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }

    private fun createShowMediaItem(it: PartialShowData) = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(it.showTitle)
                .setDisplayTitle("${it.date.toAlbumFormat()} ${it.showTitle}")
                .setReleaseYear(it.date.year)
                .setReleaseDay(it.date.dayOfMonth)
                .setReleaseMonth(it.date.monthNumber)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                .setArtworkUri(PosterUrl(it.posterUrl).toUri())
                .build()
        )
        .setMediaId(MediaId.ShowId(it))
        .build()

    private fun createYearMediaItem(
        yearId: MediaId.YearId,
        posterUrl: PosterUrl? = null
    ): MediaItemNode {
        return MediaItemNode(
            MediaItem.Builder()
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(yearId.year)
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_YEARS)
                        .setArtworkUri(posterUrl?.toUri())
                        .build()
                )
                .setMediaId(yearId)
                .build()
        )
    }


    /**
     * Retries the action every 100 milliseconds up to 3 seconds and then
     * continues to retry again forever every 3 seconds
     */
    private suspend inline fun <Result> retryForever(action: () -> Either<Throwable, Result>): Result {
        Timber.d("retryForever()")
        return Schedule.exponential<Throwable>(100.milliseconds)
            .doWhile { _, duration -> duration < 3.seconds }
            .andThen(Schedule.spaced(3.seconds))
            .retryEither(action)
            .getOrElse { error("This shouldn't happen") }
    }
}
