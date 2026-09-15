package gizz.tapes.storage

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.AppContext
import gizz.tapes.api.data.KglwFile
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Show
import gizz.tapes.data.BAND_NAME
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.RecordingId
import gizz.tapes.data.ShowId
import gizz.tapes.db.Database
import gizz.tapes.db.Files
import gizz.tapes.db.Recordings
import gizz.tapes.db.Shows
import gizz.tapes.storage.id3.Id3CoverArt
import gizz.tapes.storage.id3.Id3Tags
import gizz.tapes.storage.id3.fetchCoverArt
import gizz.tapes.storage.id3.writeMp3WithId3Tags
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer

enum class RecordingDownloadStatus { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED }

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ShowSaver(
    private val db: Database,
    private val musicDownloader: MusicDownloader,
    private val appContext: AppContext,
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : DownloadedShowsSource {

    private val logger = Logger.withTag("ShowSaver")

    // outlives any single screen/ViewModel (this is an app-wide singleton) so a track's ID3
    // tagging still happens even if the user navigates away from the show while it's downloading.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // guards against saveShow() being called twice for the same recording (it's explicitly
    // idempotent) launching two tagging coroutines for the same file that would race on the
    // same temp path and atomicMove destination - mirrors MusicDownloader's jobs[localPath] guard.
    private val taggingJobs = mutableMapOf<String, Job>()

    fun saveShow(
        recordingId: RecordingId,
        title: FullShowTitle,
        show: Show
    ) {
        val recording: Recording = show.recordings.first { it.id == recordingId.id }

        // idempotent: repeat saves of an already-saved show must not crash on the
        // recordings/shows primary keys (see Shows.sq / Recordings.sq insertOrIgnore)
        db.showsQueries.insertShow(show.toDbShow(title))

        val r = Recordings(
            id = recording.id,
            showId = show.id,
            uploadedAt = recording.uploadedAt,
            type = recording.type,
            source = recording.source,
            lineage = recording.lineage,
            taper = recording.taper
        )

        db.recordingsQueries.insertRecording(r)

        // fetched once and shared across every track's tagging job below, rather than once per
        // track - it's the same show poster for all of them.
        val coverArt = scope.async { fetchCoverArt(httpClient, show.posterUrl) }

        logger.d { "recording $recording" }
        recording.files.forEachIndexed { index, file ->
            saveFile(recording, file, index, title, show, coverArt)
        }
    }

    // one track's worth of saveShow()'s work: record it in the DB, download it if needed, and
    // tag it once downloaded.
    private fun saveFile(
        recording: Recording,
        file: KglwFile,
        index: Int,
        title: FullShowTitle,
        show: Show,
        coverArt: Deferred<Id3CoverArt?>,
    ) {
        val remoteUrl = recording.filesPathPrefix + file.filename
        val localPath = localPath(recording, file.filename)

        db.filesQueries.insertFile(
            Files(
                recordingId = recording.id,
                remoteUrl = remoteUrl,
                localPath = localPath,
                length = file.length,
                title = file.title
            )
        )

        // the DB row may already exist from a previous save, but the file itself
        // can still be missing (cleared storage, previously failed download, etc) -
        // always redownload when it's actually absent, regardless of DB state.
        if (isDownloaded(localPath)) {
            logger.d { "${file.title} already downloaded, skipping" }
        } else {
            logger.d { "Downloading ${file.title}" }
            musicDownloader.download(remoteUrl = remoteUrl, localPath = localPath)
        }

        // tags the file with the show/track metadata as soon as it's downloaded (already
        // downloaded is handled too, e.g. a show saved before ID3 tagging existed), so
        // downloaded files carry correct metadata for offline use in other apps.
        if (taggingJobs[localPath]?.isActive != true) {
            taggingJobs[localPath] = scope.launch {
                tagOnceDownloaded(localPath) {
                    Id3Tags(
                        title = file.title,
                        artist = BAND_NAME,
                        album = title.fullShowTitle.value,
                        trackNumber = index + 1,
                        trackCount = recording.files.size,
                        year = show.date.year,
                        coverArt = coverArt.await(),
                    )
                }
            }
        }
    }

    private suspend fun tagOnceDownloaded(localPath: String, buildTags: suspend () -> Id3Tags) {
        if (!isDownloaded(localPath)) {
            val succeeded = musicDownloader.downloadState(localPath)
                .first { it == DownloadState.SUCCEEDED || it == DownloadState.FAILED } == DownloadState.SUCCEEDED
            if (!succeeded) return
        }

        runCatching { tagFileInPlace(appContext.downloadsPath / localPath, buildTags()) }
            .onFailure { logger.e(it) { "Failed to write ID3 tags to $localPath" } }
    }

    private fun tagFileInPlace(path: Path, tags: Id3Tags) {
        val tmp = checkNotNull(path.parent) / "${path.name}.tagging.tmp"
        val input = fileSystem.source(path).buffer()
        val output = fileSystem.sink(tmp).buffer()
        try {
            writeMp3WithId3Tags(input, output, tags)
        } finally {
            output.close()
            input.close()
        }
        fileSystem.atomicMove(tmp, path)
    }

    // removes a downloaded recording's files from disk and its rows from the DB, so it goes
    // back to NOT_DOWNLOADED and a future saveShow() re-downloads it from scratch.
    suspend fun deleteDownloadedRecording(show: Show, recordingId: RecordingId) {
        val recording: Recording = show.recordings.first { it.id == recordingId.id }

        recording.files.forEach {
            fileSystem.delete(appContext.downloadsPath / localPath(recording, it.filename), mustExist = false)
        }
        // best-effort: only succeeds once the directory is empty, which it now should be.
        runCatching { fileSystem.delete(appContext.downloadsPath / recording.id, mustExist = false) }

        db.filesQueries.deleteFilesForRecording(recording.id)
        db.recordingsQueries.deleteRecording(recording.id)

        // without this, downloadState() keeps reporting the old completed download's SUCCEEDED
        // state forever, even after the file and DB rows above are gone.
        musicDownloader.forgetFinishedDownloads()
    }

    // the full show (setlist, notes, recordings, etc.) as it was at download time, read back
    // from the local DB - lets the show screen render fully offline instead of hitting the
    // network for a show that's already been saved.
    override fun loadCachedShow(showId: ShowId): Show? =
        db.showsQueries.selectShow(showId.value).executeAsOneOrNull()?.decodeShowData()

    // shows with at least one downloaded track - used as an offline fallback (e.g. Android Auto
    // browsing) when the network is unavailable to fetch the full show list/detail.
    override fun loadDownloadedShows(): List<Show> =
        db.showsQueries.selectDownloadedShows().executeAsList().mapNotNull { it.decodeShowData() }

    private fun Shows.decodeShowData(): Show? =
        showData?.let { runCatching { Json.decodeFromString<Show>(it) }.getOrNull() }

    // absolute on-disk path for a track if it's already downloaded, so playback can use
    // the local file instead of streaming; null if it needs to be streamed from the network.
    override fun localFileIfDownloaded(recording: Recording, filename: String): String? {
        val localPath = localPath(recording, filename)
        return if (isDownloaded(localPath)) (appContext.downloadsPath / localPath).toString() else null
    }

    private fun isDownloaded(localPath: String): Boolean = fileSystem.exists(appContext.downloadsPath / localPath)

    // combines per-file WorkManager state with an immediate on-disk check, so shows
    // downloaded in a past session (no live work info) still report as downloaded.
    fun observeRecordingDownloadStatus(show: Show, recordingId: RecordingId): Flow<RecordingDownloadStatus> {
        val recording: Recording = show.recordings.first { it.id == recordingId.id }
        if (recording.files.isEmpty()) return flowOf(RecordingDownloadStatus.NOT_DOWNLOADED)

        val perFileFlows = recording.files.map { file ->
            val localPath = localPath(recording, file.filename)
            flow {
                // once the file is on disk, it's downloaded - never let a later WorkManager
                // state (e.g. NOT_STARTED, from a past session with no live work info) undo that.
                if (isDownloaded(localPath)) {
                    emit(DownloadState.SUCCEEDED)
                } else {
                    emitAll(musicDownloader.downloadState(localPath))
                }
            }
        }

        return combine(perFileFlows) { states ->
            when {
                states.all { it == DownloadState.SUCCEEDED } -> RecordingDownloadStatus.DOWNLOADED
                states.any { it == DownloadState.IN_PROGRESS } -> RecordingDownloadStatus.DOWNLOADING
                // a failed file falls back to NOT_DOWNLOADED so the button reappears and a
                // retap retries just the missing files (saveShow only downloads what's absent).
                else -> RecordingDownloadStatus.NOT_DOWNLOADED
            }
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    private fun Show.toDbShow(title: FullShowTitle) = Shows(
        id = this.id,
        sortOrder = this.order,
        date = this.date,
        posterUrl = this.posterUrl,
        notes = this.notes,
        title = title.title.value,
        permalink = this.kglwNet.permalink,
        showData = Json.encodeToString(this)
    )
}
