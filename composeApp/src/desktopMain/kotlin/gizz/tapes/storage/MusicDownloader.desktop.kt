package gizz.tapes.storage

import arrow.core.Either
import arrow.resilience.Schedule
import arrow.resilience.retryEither
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.AppContext
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(AppScope::class)
actual class MusicDownloader(
    private val appContext: AppContext,
    private val downloadHttpClient: DownloadHttpClient,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val logger = Logger.withTag("MusicDownloader")
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val stateTracker = DownloadStateTracker()
    private val jobs = mutableMapOf<String, Job>()

    actual fun download(remoteUrl: String, localPath: String) {
        // mirrors WorkManager's ExistingWorkPolicy.KEEP on Android - a download already
        // running for this path is left alone rather than started twice.
        if (jobs[localPath]?.isActive == true) return

        logger.d { "download() remoteUrl=$remoteUrl localPath=$localPath" }
        stateTracker.markState(localPath, DownloadState.IN_PROGRESS)

        jobs[localPath] = scope.launch {
            val destination = appContext.downloadsPath / localPath
            val finalState = downloadWithRetries(remoteUrl, destination)
                .fold({ DownloadState.FAILED }, { DownloadState.SUCCEEDED })
            stateTracker.markState(localPath, finalState)
        }
    }

    actual fun downloadState(localPath: String): Flow<DownloadState> = stateTracker.downloadState(localPath)

    actual suspend fun forgetFinishedDownloads() {
        stateTracker.forgetFinishedDownloads()
        jobs.entries.removeAll { !it.value.isActive }
    }

    private suspend fun downloadWithRetries(remoteUrl: String, destination: Path): Either<Exception, Unit> {
        val schedule = Schedule.exponential<Exception>(RETRY_DELAY) and Schedule.recurs(MAX_ATTEMPTS - 1)
        return schedule.retryEither { downloadOnce(remoteUrl, destination) }
    }

    private suspend fun downloadOnce(remoteUrl: String, destination: Path): Either<Exception, Unit> {
        val parentDir = checkNotNull(destination.parent)
        val tmp = parentDir / "${destination.name}.tmp"

        return Either.catchOrThrow<Exception, Unit> {
            fileSystem.createDirectories(parentDir)
            downloadHttpClient.client.prepareGet(remoteUrl).execute { response ->
                if (!response.status.isSuccess()) {
                    error("Error downloading $remoteUrl status=${response.status}")
                }

                val channel = response.bodyAsChannel()
                fileSystem.sink(tmp).buffer().use { sink ->
                    val buffer = ByteArray(8192)
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                        if (bytesRead > 0) {
                            sink.write(buffer, 0, bytesRead)
                        }
                    }
                }
                // only make the file visible at its final path once fully written, so an
                // in-memory-only state loss (app killed mid-download) never leaves a partial
                // file that ShowSaver.isDownloaded() mistakes for a completed download.
                fileSystem.atomicMove(tmp, destination)
            }
        }.onLeft { e ->
            logger.e(e) { "Failed downloading $remoteUrl" }
            fileSystem.delete(tmp, mustExist = false)
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3L
        val RETRY_DELAY = 2.seconds
    }
}
