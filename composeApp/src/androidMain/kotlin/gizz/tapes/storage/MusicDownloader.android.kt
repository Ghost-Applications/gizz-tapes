package gizz.tapes.storage

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.await
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.AppContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

@Inject
@SingleIn(AppScope::class)
actual class MusicDownloader(private val appContext: AppContext) {

    private val logger = Logger.withTag("MusicDownloader")

    actual fun download(remoteUrl: String, localPath: String) {
        logger.d { "download() remoteUrl=$remoteUrl localPath=$localPath" }
        val destination = (appContext.downloadsPath / localPath).toString()
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                DownloadWorker.createData(
                    url = remoteUrl,
                    destinationPath = destination
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(appContext.context).enqueueUniqueWork(
            uniqueWorkName(localPath),
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )
    }

    actual fun downloadState(localPath: String): Flow<DownloadState> {
        return WorkManager.getInstance(appContext.context)
            .getWorkInfosForUniqueWorkFlow(uniqueWorkName(localPath))
            .map { it.toDownloadState() }
    }

    // WorkManager has no API to forget a single unique work's terminal state - cancelUniqueWork
    // is a no-op once a work item has finished, so a completed download's SUCCEEDED WorkInfo
    // would otherwise persist forever, even after its file is deleted. pruneWork() is global,
    // but that's fine here since WorkManager is only ever used for downloads in this app, and
    // status checks for files still on disk never consult WorkManager in the first place.
    actual suspend fun forgetFinishedDownloads() {
        WorkManager.getInstance(appContext.context).pruneWork().await()
    }

    private fun uniqueWorkName(localPath: String) = "download_$localPath"

    private fun List<WorkInfo>.toDownloadState(): DownloadState =
        when (firstOrNull()?.state) {
            null -> DownloadState.NOT_STARTED
            WorkInfo.State.SUCCEEDED -> DownloadState.SUCCEEDED
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> DownloadState.FAILED
            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.IN_PROGRESS
        }
}
