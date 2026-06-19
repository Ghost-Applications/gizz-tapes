package gizz.tapes.storage

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.AppContext
import gizz.tapes.api.data.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.buffer
import okio.sink

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidDownloadsExporter(
    private val appContext: AppContext,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : DownloadsExporter {

    private val logger = Logger.withTag("AndroidDownloadsExporter")

    override suspend fun export(recording: Recording, showFolderName: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            recording.files.forEach { file ->
                exportFile(recording = recording, filename = file.filename, showFolderName = showFolderName)
            }
        }
    }

    private fun exportFile(recording: Recording, filename: String, showFolderName: String) {
        val source = fileSystem.existingDownloadOrNull(
            appContext.downloadsPath / localPath(recording, filename),
            filename,
            logger
        ) ?: return

        val resolver = appContext.context.contentResolver
        val extension = filename.substringAfterLast('.', missingDelimiterValue = "")
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/GizzTapes/$showFolderName")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert failed for $filename")

        resolver.openOutputStream(uri)?.use { output ->
            fileSystem.source(source).buffer().use { input ->
                input.readAll(output.sink())
            }
        } ?: error("Could not open an output stream for $filename")

        val finalizeValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        resolver.update(uri, finalizeValues, null, null)
    }
}
