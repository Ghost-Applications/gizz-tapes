package gizz.tapes.storage

import gizz.tapes.api.data.Recording

fun interface DownloadsExporter {
    suspend fun export(recording: Recording, showFolderName: String): Result<Unit>
}
