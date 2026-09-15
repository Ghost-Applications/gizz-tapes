package gizz.tapes.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import arrow.core.Either
import arrow.core.getOrElse
import arrow.resilience.Schedule
import arrow.resilience.retryEither
import co.touchlab.kermit.Logger
import com.google.common.collect.ImmutableList
import dev.zacsweers.metro.Inject
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.KglwFile
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Recording
import gizz.tapes.data.BAND_NAME
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.data.Year
import gizz.tapes.nav.Destination
import gizz.tapes.storage.DownloadedShowsSource
import gizz.tapes.util.putRemoteUrl
import gizz.tapes.util.realMediaId
import gizz.tapes.util.retry
import gizz.tapes.util.setMediaId
import gizz.tapes.util.showTitle
import gizz.tapes.util.title
import gizz.tapes.util.toAlbumFormat
import gizz.tapes.util.toExtrasBundle
import gizz.tapes.util.toLocalOrRemoteUri
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.number
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Inject
class MediaItemTree(
    private val apiClient: GizzTapesApiClient,
    private val downloadedShowsSource: DownloadedShowsSource,
) {
    private val logger = Logger.withTag("MediaItemTree")

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
        logger.d { "getRoot()" }
        return root
    }

    @OptIn(UnstableApi::class)
    suspend fun getItem(id: MediaId): MediaItem {
        logger.d { "getItem() id=$id" }

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
        logger.d { "getChildren() parentId=$parentId" }

        return when (parentId) {
            MediaId.RootId -> loadChildrenForRoot()
            MediaId.DownloadedShowsId -> loadChildrenForDownloadedShows()
            is MediaId.YearId -> loadChildrenForYear(parentId)
            is MediaId.ShowId -> loadChildrenForShow(parentId)
            is MediaId.RecordingId -> loadChildrenForRecording(parentId)
            else -> {
                logger.w { "No children for parentId=$parentId" }
                ImmutableList.of()
            }
        }
    }

    private suspend fun loadChildrenForRoot(): ImmutableList<MediaItem> {
        val partialShows = fetchShows()
        val years: List<MediaItemNode> = if (partialShows != null) {
            partialShows
                .groupBy { it.date.year }
                .map { (key, value) -> Year(key) to PosterUrl(value.random().posterUrl) }
                .reversed()
                .map { (year, posterUrl) -> createYearMediaItem(MediaId.YearId(year.value), posterUrl) }
        } else {
            logger.w { "loadChildrenForRoot() couldn't reach the network - falling back to downloaded shows" }
            downloadedShows()
                .groupBy { it.date.year }
                .map { (key, value) -> Year(key) to PosterUrl(value.random().posterUrl) }
                .reversed()
                .map { (year, posterUrl) -> createYearMediaItem(MediaId.YearId(year.value), posterUrl) }
        }

        // the "Downloaded Shows" entry isn't actually a year, but it's a root-level sibling of the
        // year folders, so it's cached in the same `years` map keyed by its own MediaId - lookups
        // by MediaId (e.g. loadChildrenForYear's getOrLoadParent) don't care which kind it is.
        val allNodes = listOf(MediaItemNode(createDownloadedShowsMediaItem())) + years

        allNodes.forEach {
            this@MediaItemTree.years[it.mediaId] = it
        }

        return ImmutableList.copyOf(allNodes.map { it.item })
    }

    // always reloaded straight from disk (not cached) - it's a local DB read, not network, so
    // there's no cost to staying fresh, and it should reflect a show finishing its download
    // without waiting for a restart.
    private fun loadChildrenForDownloadedShows(): ImmutableList<MediaItem> {
        val downloaded = downloadedShows()
        logger.d { "loadChildrenForDownloadedShows() downloaded.size=${downloaded.size} ids=${downloaded.map { it.id }}" }

        val showNodes = downloaded
            .sortedByDescending { it.date }
            .map { createShowMediaItemFromCached(it) }
            .map { MediaItemNode(it) }

        showNodes.forEach { shows[it.mediaId] = it }

        logger.d { "loadChildrenForDownloadedShows() returning ${showNodes.size} items" }
        return ImmutableList.copyOf(showNodes.map { it.item })
    }

    // looks up [id] in [nodes]; if absent, loads its parent (which populates [nodes] as a side
    // effect) and looks up again - shared by loadChildrenForYear/Show/Recording below.
    private suspend fun getOrLoadParent(
        nodes: Map<MediaId, MediaItemNode>,
        id: MediaId,
        loadParent: suspend () -> Unit,
    ): MediaItemNode = nodes[id] ?: run {
        loadParent()
        nodes[id] ?: error("Something went wrong loading parents")
    }

    private suspend fun loadChildrenForYear(yearId: MediaId.YearId): ImmutableList<MediaItem> {
        val year = getOrLoadParent(years, yearId) { loadChildrenForRoot() }
        logger.d { "year=$year" }

        if (year.children.isEmpty()) {
            val partialShows = fetchShows()
            val shows = if (partialShows != null) {
                partialShows
                    .filter { it.date.year.toString() == year.mediaId.year }
                    .reversed()
                    .map { createShowMediaItemFromListing(it) }
                    .map { MediaItemNode(item = it) }
            } else {
                logger.w { "loadChildrenForYear() couldn't reach the network - falling back to downloaded shows" }
                downloadedShows()
                    .filter { it.date.year.toString() == year.mediaId.year }
                    .reversed()
                    .map { createShowMediaItemFromCached(it) }
                    .map { MediaItemNode(item = it) }
            }

            shows.forEach {
                this@MediaItemTree.shows[it.mediaId] = it
            }
            year.children.addAll(shows)
        }

        return ImmutableList.copyOf(year.children.map { it.item })
    }

    @OptIn(UnstableApi::class)
    private suspend fun loadChildrenForShow(showId: MediaId.ShowId): ImmutableList<MediaItem> {
        val show = getOrLoadParent(shows, showId) { loadChildrenForYear(showId.parent) }

        if (show.children.isEmpty()) {
            val showData = loadShow(checkNotNull(show.mediaId.showId))

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
                                .setReleaseDay(showData.date.day)
                                .setReleaseMonth(showData.date.month.number)
                                .setAlbumArtist(BAND_NAME)
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
        logger.d { "loadChildrenForRecording() recordingId=$recordingId" }
        val recording = getOrLoadParent(recordings, recordingId) { loadChildrenForShow(recordingId.parent) }
        logger.d { "loadChildrenForRecording() recordingInTree=$recording" }

        if (recording.children.isEmpty()) {
            val showData = loadShow(checkNotNull(recording.mediaId.showId))

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
        logger.d { "createTrackMediaItem() recording=$recording, track=$track" }
        val remoteUrl = recording.filesPathPrefix + track.filename
        val localPath = downloadedShowsSource.localFileIfDownloaded(recording, track.filename)
        val builder = MediaItem.Builder()
        // only build a Uri (file://) when there's a local download to point to - passing the
        // remote URL through as a plain String, like before, avoids parsing it into a Uri here.
        if (localPath != null) builder.setUri(localPath.toLocalOrRemoteUri()) else builder.setUri(remoteUrl)
        return builder
            .setMediaId(MediaId.TrackId(show = showData, file = track, recording = recording))
            .setMediaMetadata(
                // I think there's a reason for using `show.item.title` but I don't remember now
                // figure out when testing on a real device and leave a good comment...
                MediaMetadata.Builder()
                    .setExtras(
                        Destination.Show(
                            id = ShowId(checkNotNull(show.mediaId.showId) { "Show media id is null" }),
                            title = FullShowTitle(
                                title = Title(show.item.title),
                                date = showData.date
                            )
                        ).toExtrasBundle().putRemoteUrl(remoteUrl)
                    )
                    .setArtist("$dateString ${show.item.title}")
                    .setAlbumArtist(BAND_NAME)
                    .setAlbumTitle(show.item.title)
                    .setTitle(track.title)
                    .setRecordingYear(showData.date.year)
                    .setRecordingMonth(showData.date.month.number)
                    .setRecordingDay(showData.date.day)
                    .setArtworkUri(PosterUrl(showData.posterUrl).toUri())
                    .setDurationMs(track.length.inWholeMilliseconds)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }

    // network listing case (root/year, when the API's shows() listing succeeded).
    private fun createShowMediaItemFromListing(it: PartialShowData) = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(it.showTitle)
                .setDisplayTitle("${it.date.toAlbumFormat()} ${it.showTitle}")
                .setReleaseYear(it.date.year)
                .setReleaseDay(it.date.day)
                .setReleaseMonth(it.date.month.number)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                .setArtworkUri(PosterUrl(it.posterUrl).toUri())
                .build()
        )
        .setMediaId(MediaId.ShowId(it))
        .build()

    // cached/downloaded-show case: the Downloaded Shows folder, or a root/year fallback when the
    // network listing failed - only the full cached Show is available here, not PartialShowData
    // (which comes from a separate listing endpoint).
    private fun createShowMediaItemFromCached(show: gizz.tapes.api.data.Show) = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(show.title.orEmpty())
                .setDisplayTitle("${show.date.toAlbumFormat()} ${show.title.orEmpty()}")
                .setReleaseYear(show.date.year)
                .setReleaseDay(show.date.day)
                .setReleaseMonth(show.date.month.number)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                .setArtworkUri(PosterUrl(show.posterUrl).toUri())
                .build()
        )
        .setMediaId(MediaId.ShowId(show))
        .build()

    private fun createDownloadedShowsMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Downloaded Shows")
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build()
        )
        .setMediaId(MediaId.DownloadedShowsId)
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

    private fun PosterUrl.toUri() = Uri.parse(value)

    // shows with at least one downloaded track - what the Downloaded Shows folder lists.
    private fun downloadedShows(): List<gizz.tapes.api.data.Show> = downloadedShowsSource.loadDownloadedShows()

    // root/year listing has no complete cached catalog to prefer (only downloaded shows are
    // cached, which is a subset), so unlike loadShow() below it can't just wait forever - a
    // bounded 3-attempt/~5s budget, then null so the caller falls back to downloaded shows.
    private suspend fun fetchShows(): List<PartialShowData>? =
        withTimeoutOrNull(5.seconds) { retry(times = 3) { apiClient.shows() } }?.getOrNull()

    // prefers the cached copy of an already-downloaded show (so browsing a downloaded show works
    // fully offline, e.g. via the Downloaded Shows folder), falling back to the live API -
    // retried forever - for anything not downloaded yet.
    private suspend fun loadShow(showId: String): gizz.tapes.api.data.Show =
        downloadedShowsSource.loadCachedShow(ShowId(showId)) ?: retryForever { apiClient.show(showId) }

    /**
     * Retries the action every 100 milliseconds up to 3 seconds and then
     * continues to retry again forever every 3 seconds
     */
    private suspend inline fun <Result> retryForever(action: () -> Either<Throwable, Result>): Result {
        logger.d { "retryForever()" }
        return Schedule.exponential<Throwable>(100.milliseconds)
            .doWhile { _, duration -> duration < 3.seconds }
            .andThen(Schedule.spaced(3.seconds))
            .retryEither(action)
            // the schedule above never gives up (spaced(3.seconds) retries indefinitely), so
            // retryEither can only ever produce a Right - this branch is unreachable.
            .getOrElse { error("This shouldn't happen") }
    }
}
