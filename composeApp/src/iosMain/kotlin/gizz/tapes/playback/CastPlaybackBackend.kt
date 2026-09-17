package gizz.tapes.playback

import cocoapods.google_cast_sdk.GCKCastSession
import cocoapods.google_cast_sdk.GCKImage
import cocoapods.google_cast_sdk.GCKMediaInformation
import cocoapods.google_cast_sdk.GCKMediaInformationBuilder
import cocoapods.google_cast_sdk.GCKMediaLoadRequestDataBuilder
import cocoapods.google_cast_sdk.GCKMediaMetadata
import cocoapods.google_cast_sdk.GCKMediaPlayerStateBuffering
import cocoapods.google_cast_sdk.GCKMediaPlayerStateIdle
import cocoapods.google_cast_sdk.GCKMediaPlayerStatePlaying
import cocoapods.google_cast_sdk.GCKMediaQueueDataBuilder
import cocoapods.google_cast_sdk.GCKMediaQueueItemBuilder
import cocoapods.google_cast_sdk.GCKMediaQueueTypeGeneric
import cocoapods.google_cast_sdk.GCKMediaRepeatModeOff
import cocoapods.google_cast_sdk.GCKMediaSeekOptions
import cocoapods.google_cast_sdk.GCKMediaStatus
import cocoapods.google_cast_sdk.GCKMediaStreamTypeBuffered
import cocoapods.google_cast_sdk.GCKRemoteMediaClient
import cocoapods.google_cast_sdk.GCKRemoteMediaClientListenerProtocol
import cocoapods.google_cast_sdk.kGCKMetadataKeyAlbumTitle
import cocoapods.google_cast_sdk.kGCKMetadataKeyTitle
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.ui.player.MediaDurationInfo
import gizz.tapes.ui.player.PlayerState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.darwin.NSObject

// Same customData keys androidApp's full-flavor GizzMediaItemConverter uses for
// recordingYear/Month/Day, so a session started on one platform can be reconstructed on the
// other. showId/showTitle have no Android Cast wire representation (Android reconstructs those
// from its own MediaBrowser mediaId scheme instead), so a session picked up from Android falls
// back to a synthetic showId built from the content id.
private const val KEY_RECORDING_YEAR = "recordingYear"
private const val KEY_RECORDING_MONTH = "recordingMonth"
private const val KEY_RECORDING_DAY = "recordingDay"
private const val KEY_SHOW_ID = "showId"

/**
 * Drives playback on an active Cast session via GCKRemoteMediaClient. Constructed by
 * IosMediaPlayer when a Cast session starts/resumes, then wired to the session via [attach].
 * Released when the session ends.
 *
 * Doesn't implement GCKRemoteMediaClientListenerProtocol directly - Kotlin/Native doesn't support
 * mixing that particular protocol with a plain Kotlin interface on one class (unlike e.g.
 * GCKSessionManagerListenerProtocol, which is fine - see IosMediaPlayer). Forwards status updates
 * from a small NSObject-only [MediaStatusListener] instead, same pattern as
 * BackgroundDownloadSession's SessionDelegate.
 */
@OptIn(ExperimentalForeignApi::class)
class CastPlaybackBackend : PlaybackBackend {

    private var remoteMediaClient: GCKRemoteMediaClient? = null
    private val listener = MediaStatusListener { status ->
        status?.let { updateState(it) } ?: run { _state.value = PlayerState.NoMedia }
    }

    private val _state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    override val currentPosition: Long
        get() = ((remoteMediaClient?.approximateStreamPosition() ?: 0.0) * 1000.0).toLong()

    private var playlist: List<PlaybackItem> = emptyList()

    fun attach(castSession: GCKCastSession) {
        val client = castSession.remoteMediaClient()
        remoteMediaClient = client
        client?.addListener(listener)
        client?.mediaStatus?.let { updateState(it) }
    }

    override fun setPlaylist(items: List<PlaybackItem>, startIndex: Int) {
        playlist = items
        val clampedStart = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        val queueItems = items.map { item ->
            GCKMediaQueueItemBuilder().apply {
                setMediaInformation(item.toCastMediaInformation())
                setAutoplay(true)
            }.build()
        }
        val queueData = GCKMediaQueueDataBuilder(queueType = GCKMediaQueueTypeGeneric).apply {
            setItems(queueItems)
            setStartIndex(clampedStart.toULong())
            setRepeatMode(GCKMediaRepeatModeOff)
        }.build()
        val requestData = GCKMediaLoadRequestDataBuilder().apply {
            setQueueData(queueData)
            setAutoplay(NSNumber(bool = true))
        }.build()
        remoteMediaClient?.loadMediaWithLoadRequestData(requestData)
    }

    override fun play() {
        remoteMediaClient?.play()
    }

    override fun pause() {
        remoteMediaClient?.pause()
    }

    override fun seekTo(index: Int, positionMs: Long) {
        val client = remoteMediaClient ?: return
        if (index != currentItemIndex() && index in playlist.indices) {
            val itemId = client.mediaStatus?.queueItemAtIndex(index.toULong())?.itemID()
            if (itemId != null) {
                client.queueJumpToItemWithID(itemId, customData = null)
            }
        }
        val options = GCKMediaSeekOptions().apply { interval = positionMs / 1000.0 }
        client.seekWithOptions(options)
    }

    override fun skipToPrevious() {
        remoteMediaClient?.queuePreviousItem()
    }

    override fun skipToNext() {
        remoteMediaClient?.queueNextItem()
    }

    override fun release() {
        remoteMediaClient?.removeListener(listener)
    }

    override fun snapshot(): PlaybackSnapshot = PlaybackSnapshot(
        items = playlist,
        currentIndex = currentItemIndex().coerceAtLeast(0),
        isPlaying = remoteMediaClient?.mediaStatus?.playerState() == GCKMediaPlayerStatePlaying,
        positionMs = currentPosition,
    )

    private fun currentItemIndex(): Int {
        val status = remoteMediaClient?.mediaStatus ?: return 0
        val currentItemId = status.currentItemID()
        val count = status.queueItemCount().toInt()
        for (i in 0 until count) {
            if (status.queueItemAtIndex(i.toULong())?.itemID() == currentItemId) return i
        }
        return 0
    }

    private fun updateState(mediaStatus: GCKMediaStatus) {
        val playlistItem = playlist.getOrNull(currentItemIndex())
        val info = mediaStatus.mediaInformation
        if (playlistItem == null && info == null) {
            _state.value = PlayerState.NoMedia
            return
        }

        val positionMs = currentPosition
        val durationMs = ((info?.streamDuration() ?: 0.0) * 1000.0).toLong()
            .takeIf { it > 0 } ?: (playlistItem?.durationMs ?: 0L)

        val title = playlistItem?.title ?: info?.metadata?.stringForKey(kGCKMetadataKeyTitle) ?: "--"
        val albumTitle = playlistItem?.albumTitle ?: info?.metadata?.stringForKey(kGCKMetadataKeyAlbumTitle) ?: "--"
        val (showId, showTitle) = resolveShowInfo(playlistItem, info)

        val playerState = mediaStatus.playerState()
        val isPlaying = playerState == GCKMediaPlayerStatePlaying
        val isLoading = playerState == GCKMediaPlayerStateBuffering || playerState == GCKMediaPlayerStateIdle

        _state.value = PlayerState.MediaLoaded(
            isPlaying = isPlaying,
            isLoading = isLoading,
            showId = showId,
            showTitle = showTitle,
            durationInfo = MediaDurationInfo(positionMs, durationMs),
            artworkUri = playlistItem?.artworkUrl,
            title = title,
            albumTitle = albumTitle,
            mediaId = playlistItem?.id ?: info?.contentID().orEmpty(),
            currentTrackIndex = currentItemIndex(),
        )
    }

    // Falls back to reconstructing showId/showTitle from Cast customData when this session was
    // started by another device (e.g. Android), which doesn't know about iOS's PlaybackItem.
    private fun resolveShowInfo(
        playlistItem: PlaybackItem?,
        info: GCKMediaInformation?,
    ): Pair<ShowId, FullShowTitle> {
        if (playlistItem != null) return playlistItem.showId to playlistItem.showTitle

        @Suppress("UNCHECKED_CAST")
        val customData = info?.customData() as? Map<Any?, Any?>
        val year = (customData?.get(KEY_RECORDING_YEAR) as? Number)?.toInt()
        val month = (customData?.get(KEY_RECORDING_MONTH) as? Number)?.toInt()
        val day = (customData?.get(KEY_RECORDING_DAY) as? Number)?.toInt()
        val showId = (customData?.get(KEY_SHOW_ID) as? String) ?: info?.contentID().orEmpty()
        val albumTitle = info?.metadata?.stringForKey(kGCKMetadataKeyAlbumTitle) ?: "--"
        val date = if (year != null && month != null && day != null) LocalDate(year, month, day) else LocalDate(1, 1, 1)
        return ShowId(showId) to FullShowTitle(title = Title(albumTitle), date = date)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class MediaStatusListener(
    private val onUpdate: (GCKMediaStatus?) -> Unit,
) : NSObject(), GCKRemoteMediaClientListenerProtocol {
    override fun remoteMediaClient(client: GCKRemoteMediaClient, didUpdateMediaStatus: GCKMediaStatus?) {
        onUpdate(didUpdateMediaStatus)
    }
}

// Always uses remoteUrl (never the local file:// download path) since this only ever runs when
// queuing to Cast - a receiver device can't fetch an app-private local file, mirroring how
// androidApp's GizzMediaItemConverter substitutes it in for downloaded tracks.
@OptIn(ExperimentalForeignApi::class)
internal fun PlaybackItem.toCastMediaInformation(): GCKMediaInformation {
    val metadata = GCKMediaMetadata()
    metadata.setString(title, forKey = kGCKMetadataKeyTitle)
    metadata.setString(albumTitle, forKey = kGCKMetadataKeyAlbumTitle)
    artworkUrl?.let { url ->
        NSURL.URLWithString(url)?.let { nsUrl ->
            // GCKImage requires explicit dimensions up front (it doesn't fetch the image to
            // measure it) - 480x480 is just a reasonable placeholder; the receiver still scales
            // to the image's real aspect ratio when it actually loads it.
            metadata.addImage(GCKImage(uRL = nsUrl, width = 480, height = 480))
        }
    }

    // Built as a real NSMutableDictionary (not a raw Kotlin Map) since GCKMediaInformationBuilder
    // needs to JSON-serialize customData for transmission to the receiver - a Kotlin Map passed
    // through the untyped `id customData` property doesn't bridge to NSDictionary automatically.
    // The explicit `as NSString` casts look redundant (Kotlin String bridges to NSString) but
    // are required here: setObject(forKey:) takes a generic NSCopyingProtocol parameter, and the
    // compiler doesn't apply that bridging conversion through a protocol-typed parameter.
    val customData = NSMutableDictionary().apply {
        setObject(NSNumber(int = showDate.year), forKey = KEY_RECORDING_YEAR as NSString)
        setObject(NSNumber(int = showDate.month.number), forKey = KEY_RECORDING_MONTH as NSString)
        setObject(NSNumber(int = showDate.day), forKey = KEY_RECORDING_DAY as NSString)
        setObject(showId.value, forKey = KEY_SHOW_ID as NSString)
    }

    return GCKMediaInformationBuilder(contentID = remoteUrl).apply {
        streamType = GCKMediaStreamTypeBuffered
        contentType = "audio/mpeg"
        setMetadata(metadata)
        setStreamDuration(durationMs / 1000.0)
        setCustomData(customData)
    }.build()
}
