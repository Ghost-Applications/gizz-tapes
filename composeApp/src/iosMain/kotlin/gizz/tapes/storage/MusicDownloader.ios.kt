package gizz.tapes.storage

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

// thin Metro-visible facade over BackgroundDownloadSession, which owns the actual NSURLSession
// (background sessions must live outside Metro's DI graph - see BackgroundDownloadSession's doc).
@Inject
@SingleIn(AppScope::class)
actual class MusicDownloader {
    actual fun download(remoteUrl: String, localPath: String) =
        BackgroundDownloadSession.download(remoteUrl, localPath)

    actual fun downloadState(localPath: String): Flow<DownloadState> =
        BackgroundDownloadSession.downloadState(localPath)

    actual suspend fun forgetFinishedDownloads() = BackgroundDownloadSession.forgetFinishedDownloads()
}
