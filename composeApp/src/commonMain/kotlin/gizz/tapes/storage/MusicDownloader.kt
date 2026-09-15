package gizz.tapes.storage

import kotlinx.coroutines.flow.Flow

enum class DownloadState { NOT_STARTED, IN_PROGRESS, SUCCEEDED, FAILED }

expect class MusicDownloader {
    // localPath is relative to the platform's AppContext.downloadsPath
    fun download(remoteUrl: String, localPath: String)
    fun downloadState(localPath: String): Flow<DownloadState>

    // clears any finished (SUCCEEDED/FAILED) download bookkeeping, so a file deleted
    // outside of a download's normal lifecycle doesn't keep reporting stale state.
    suspend fun forgetFinishedDownloads()
}
