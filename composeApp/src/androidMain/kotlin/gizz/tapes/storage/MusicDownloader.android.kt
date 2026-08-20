package gizz.tapes.storage

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.TimeUnit

@Inject
@SingleIn(AppScope::class)
actual class MusicDownloader(val applicationContext: Context) {

    private val logger = Logger.withTag("MusicDownloader")

    actual fun download(uri: String) {
        logger.d { "download() uri=$uri" }
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                DownloadWorker.createData(
                    id = "id",
                    url = uri
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "download_${uri}",
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )
    }
}
