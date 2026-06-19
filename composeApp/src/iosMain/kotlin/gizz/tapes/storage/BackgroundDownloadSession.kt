package gizz.tapes.storage

import arrow.core.Either
import co.touchlab.kermit.Logger
import gizz.tapes.AppContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.darwin.NSObject
import kotlin.concurrent.AtomicReference
import kotlin.time.Duration.Companion.seconds

// Lives outside Metro's DI graph on purpose: a background-only relaunch (the OS waking the app
// solely to deliver a finished/failed download) may never construct the Compose UI, so Metro's
// IosAppGraph - and the MusicDownloader instance it owns - may never exist. A plain Kotlin object
// is lazily, thread-safely initialized on first touch regardless of whether Metro ever runs.
//
// Kotlin/Native's IR backend doesn't support an `object` directly subclassing an Obj-C class
// (NSObject) and implementing a protocol - only a regular `class` can - hence the separate
// SessionDelegate below, which this object owns a single instance of.
object BackgroundDownloadSession {
    private const val SESSION_IDENTIFIER = "gizz.tapes.downloads"
    private const val MAX_ATTEMPTS = 3
    private val RETRY_DELAY = 2.seconds

    private val logger = Logger.withTag("BackgroundDownloadSession")
    private val appContext = AppContext()
    private val fileSystem: FileSystem = FileSystem.SYSTEM
    private val stateTracker = DownloadStateTracker()

    // written from handleEventsForBackgroundURLSession (called from AppDelegate, main thread) and
    // read/cleared from handleFinishedEvents (the session's delegate queue thread, since
    // delegateQueue = null below) - an AtomicReference guarantees the write is visible across
    // threads and getAndSet lets the read+clear happen as one atomic step.
    private val pendingCompletionHandler = AtomicReference<(() -> Unit)?>(null)

    // background sessions already wait for/retry through connectivity gaps on their own (up to
    // NSURLSessionConfiguration.timeoutIntervalForResource, 7 days by default) - this is only for
    // genuine failures (bad HTTP status, transport error), mirroring Desktop's bounded retry.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val attempts = mutableMapOf<String, Int>()

    private val session: NSURLSession by lazy {
        val config = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(SESSION_IDENTIFIER)
        NSURLSession.sessionWithConfiguration(config, delegate = SessionDelegate(), delegateQueue = null).also { s ->
            // reconnect to any tasks still running from before this process existed, so download()'s
            // dedup check and downloadState() both reflect reality immediately after a relaunch.
            s.getTasksWithCompletionHandler { _, _, downloadTasks ->
                downloadTasks?.filterIsInstance<NSURLSessionDownloadTask>()?.forEach { task ->
                    task.taskDescription?.let { localPath -> stateTracker.markState(
                        localPath,
                        DownloadState.IN_PROGRESS
                    ) }
                }
            }
        }
    }

    fun download(remoteUrl: String, localPath: String) {
        // mirrors Android's ExistingWorkPolicy.KEEP - a download already running for this path
        // (including one resumed from before a relaunch) is left alone.
        if (stateTracker.currentState(localPath) == DownloadState.IN_PROGRESS) return

        val url = NSURL.URLWithString(remoteUrl)
        if (url == null) {
            logger.e { "Invalid download URL: $remoteUrl" }
            stateTracker.markState(localPath, DownloadState.FAILED)
            return
        }

        logger.d { "download() remoteUrl=$remoteUrl localPath=$localPath" }
        stateTracker.markState(localPath, DownloadState.IN_PROGRESS)
        session.downloadTaskWithURL(url).apply {
            taskDescription = localPath
            resume()
        }
    }

    fun downloadState(localPath: String): Flow<DownloadState> = stateTracker.downloadState(localPath)

    fun forgetFinishedDownloads() {
        stateTracker.forgetFinishedDownloads()
    }

    // called from Swift's AppDelegate.application(_:handleEventsForBackgroundURLSession:completionHandler:)
    fun handleEventsForBackgroundURLSession(identifier: String, completionHandler: () -> Unit) {
        if (identifier != SESSION_IDENTIFIER) return
        pendingCompletionHandler.value = completionHandler
        session // touch the lazy val so a background-only relaunch reconnects immediately
    }

    internal fun handleFinishedDownload(downloadTask: NSURLSessionDownloadTask, tempFileUrl: NSURL) {
        val localPath = downloadTask.taskDescription
        if (localPath == null) {
            logger.e { "Finished download task with no taskDescription, dropping" }
            return
        }

        val statusCode = (downloadTask.response as? NSHTTPURLResponse)?.statusCode
        if (statusCode == null || statusCode !in 200..299) {
            logger.e { "Download failed for $localPath: HTTP $statusCode" }
            retryOrFail(originalUrl(downloadTask), localPath)
            return
        }

        val destination = appContext.downloadsPath / localPath
        val tempPath = checkNotNull(tempFileUrl.path).toPath()
        val moved = Either.catchOrThrow<Exception, Unit> {
            fileSystem.createDirectories(checkNotNull(destination.parent))
            fileSystem.atomicMove(tempPath, destination)
        }.onLeft { e -> logger.e(e) { "Failed moving downloaded file for $localPath" } }

        attempts.remove(localPath)
        stateTracker.markState(localPath, moved.fold({ DownloadState.FAILED }, { DownloadState.SUCCEEDED }))
    }

    internal fun handleTaskCompletion(task: NSURLSessionTask, error: NSError?) {
        val localPath = task.taskDescription ?: return
        if (error != null) {
            logger.e { "Download error for $localPath: ${error.localizedDescription}" }
            retryOrFail(originalUrl(task), localPath)
        }
        // success case: state was already set to SUCCEEDED/FAILED in handleFinishedDownload above
    }

    private fun originalUrl(task: NSURLSessionTask): String? = task.originalRequest?.URL?.absoluteString

    private fun retryOrFail(remoteUrl: String?, localPath: String) {
        if (remoteUrl == null) {
            logger.e { "Cannot retry $localPath - no original request URL available" }
            attempts.remove(localPath)
            stateTracker.markState(localPath, DownloadState.FAILED)
            return
        }

        val attempt = (attempts[localPath] ?: 0) + 1
        if (attempt > MAX_ATTEMPTS) {
            logger.e { "Giving up on $localPath after $MAX_ATTEMPTS attempts" }
            attempts.remove(localPath)
            stateTracker.markState(localPath, DownloadState.FAILED)
            return
        }

        attempts[localPath] = attempt
        scope.launch {
            delay(RETRY_DELAY * attempt)
            download(remoteUrl, localPath)
        }
    }

    internal fun handleFinishedEvents() {
        val handler = pendingCompletionHandler.getAndSet(null)
        handler?.let { NSOperationQueue.mainQueue.addOperationWithBlock(it) }
    }

    private class SessionDelegate : NSObject(), NSURLSessionDownloadDelegateProtocol {
        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didFinishDownloadingToURL: NSURL
        ) = BackgroundDownloadSession.handleFinishedDownload(downloadTask, didFinishDownloadingToURL)

        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) =
            BackgroundDownloadSession.handleTaskCompletion(task, didCompleteWithError)

        override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) =
            BackgroundDownloadSession.handleFinishedEvents()
    }
}
