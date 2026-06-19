package gizz.tapes.storage

import co.touchlab.kermit.Logger
import gizz.tapes.api.data.Recording
import gizz.tapes.util.sanitizeFileName
import okio.FileSystem
import okio.Path

// Where a downloaded track lives on disk, relative to AppContext.downloadsPath - shared between
// ShowSaver (which writes it) and the per-platform DownloadsExporter implementations (which read
// it back out for export). Sanitized since recording.id/filename come from the network.
fun localPath(recording: Recording, filename: String) =
    "${sanitizeFileName(recording.id)}/${sanitizeFileName(filename)}"

// Shared by the per-platform DownloadsExporter implementations - each has its own control-flow
// shape for skipping a not-yet-downloaded file (return, return@forEach, return null), so this
// returns nullable and lets the caller branch with its own idiom via `?:`.
fun FileSystem.existingDownloadOrNull(path: Path, filename: String, logger: Logger): Path? {
    if (!exists(path)) {
        logger.e { "Cannot export $filename, it has not been downloaded" }
        return null
    }
    return path
}
