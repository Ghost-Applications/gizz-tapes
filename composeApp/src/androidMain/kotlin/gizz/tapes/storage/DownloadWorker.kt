package gizz.tapes.storage

import android.content.Context
import androidx.core.text.htmlEncode
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File

class DownloadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val logger = Logger.withTag("DownloadWorker")

    companion object {
        private const val INPUT_URL_KEY = "url"
        private const val INPUT_ID_KEY = "id"

        private const val MAX_RETRIES = 3

        fun createData(id: String, url: String): Data {
            return Data.Builder()
                .putString(INPUT_ID_KEY, id)
                .putString(INPUT_URL_KEY, url)
                .build()
        }

        private val httpClient by lazy { HttpClient() }
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(INPUT_URL_KEY) as String
        logger.d { "doWork() url=$url" }

        val destination = File(appContext.filesDir, url.htmlEncode())
        destination.parentFile?.mkdirs()

        return httpClient.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) {
                logger.e {
                    "Error downloading the file $url status=${response.status.description}"
                }

                if (runAttemptCount > MAX_RETRIES) {
                    return@execute Result.failure()
                }
                return@execute Result.retry()
            }

            val channel = response.bodyAsChannel()
            destination.outputStream().use { output ->
                val buffer = ByteArray(8192)
                while(!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            Result.success()
        }
    }
}
