package gizz.tapes.playback

import com.google.android.gms.cast.MediaError
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient

/**
 * Reports tracks the Cast receiver fails to load. media3's RemoteCastPlayer never surfaces
 * these (its getPlayerError() is always null) - the receiver just silently skips to the next
 * queue item, so without this the user only sees the cast "no media loaded" screen.
 */
internal class CastErrorMonitor(
    private val errorReporter: PlaybackErrorReporter,
) : RemoteMediaClient.Callback() {

    private var client: RemoteMediaClient? = null

    // The receiver skips a failed track, so a flaky host can fail many in a row - only surface
    // the first failure until something plays again.
    private var errorReported = false

    // By the time the receiver reports a failure it has usually already reset its current/loading
    // item ids, so remember the last item it was working on.
    private var lastItemId = MediaQueueItem.INVALID_ITEM_ID

    fun start(castContext: CastContext) {
        val sessionManager = castContext.sessionManager
        sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
        sessionManager.currentCastSession?.let(::attach)
    }

    private fun attach(session: CastSession) {
        detach()
        client = session.remoteMediaClient?.also { it.registerCallback(this) }
    }

    private fun detach() {
        client?.unregisterCallback(this)
        client = null
        errorReported = false
    }

    override fun onStatusUpdated() {
        val status = client?.mediaStatus ?: return
        val activeItemId = if (status.loadingItemId != MediaQueueItem.INVALID_ITEM_ID) {
            status.loadingItemId
        } else {
            status.currentItemId
        }
        if (activeItemId != MediaQueueItem.INVALID_ITEM_ID) lastItemId = activeItemId
        if (status.playerState == MediaStatus.PLAYER_STATE_PLAYING) {
            errorReported = false
        } else if (
            status.playerState == MediaStatus.PLAYER_STATE_IDLE &&
            status.idleReason == MediaStatus.IDLE_REASON_ERROR
        ) {
            report()
        }
    }

    override fun onMediaError(mediaError: MediaError) {
        report()
    }

    private fun report() {
        if (errorReported) return
        errorReported = true
        val media: MediaInfo? = client?.mediaStatus?.getQueueItemById(lastItemId)?.media
            ?: client?.mediaInfo
        val title = media?.metadata?.getString(MediaMetadata.KEY_TITLE)
        val url = media?.contentUrl ?: media?.contentId
        val message = if (isArchiveOrgUrl(url)) {
            archiveOrgErrorMessage(title)
        } else {
            "Chromecast couldn't play ${title?.let { "\"$it\"" } ?: "this track"}"
        }
        errorReporter.report(message)
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = attach(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = attach(session)
        override fun onSessionEnded(session: CastSession, error: Int) = detach()
        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionStartFailed(session: CastSession, error: Int) = Unit
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumeFailed(session: CastSession, error: Int) = Unit
    }
}
