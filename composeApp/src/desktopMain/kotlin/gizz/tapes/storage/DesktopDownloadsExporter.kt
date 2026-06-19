package gizz.tapes.storage

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
import okio.Path.Companion.toPath
import okio.buffer

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DesktopDownloadsExporter(
    private val appContext: AppContext,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : DownloadsExporter {

    private val logger = Logger.withTag("DesktopDownloadsExporter")

    override suspend fun export(recording: Recording, showFolderName: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val destinationDir = "${System.getProperty("user.home")}/Downloads/GizzTapes/$showFolderName".toPath()
            fileSystem.createDirectories(destinationDir)

            recording.files.forEach { file ->
                val source = fileSystem.existingDownloadOrNull(
                    appContext.downloadsPath / localPath(recording, file.filename),
                    file.filename,
                    logger
                ) ?: return@forEach

                fileSystem.source(source).buffer().use { input ->
                    fileSystem.sink(destinationDir / file.filename).buffer().use { output ->
                        input.readAll(output)
                    }
                }
            }
        }
    }
}
