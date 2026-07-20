package gizz.tapes.storage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import arrow.core.raise.recover
import co.touchlab.kermit.Logger

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
): CoroutineWorker(appContext, workerParams) {

    private val logger = Logger.withTag("DownloadWorker")

    private companion object {
        private val INPUT_URL_KEY = "url"
        private val INPUT_ID_KEY = "id"
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(INPUT_ID_KEY)
        logger.d { "doWork() url=$url" }
        return Result.success()
    }
}
