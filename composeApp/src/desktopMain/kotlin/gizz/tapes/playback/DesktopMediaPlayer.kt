package gizz.tapes.playback

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.ui.player.MediaDurationInfo
import gizz.tapes.ui.player.PlayerError
import gizz.tapes.ui.player.PlayerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DesktopMediaPlayer : GizzMediaPlayer {

    private val logger = Logger.withTag("DesktopMediaPlayer")

    private val _state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    override val currentPosition: Long get() = currentPositionMs

    @Volatile private var playlist: List<PlaybackItem> = emptyList()

    @Volatile private var currentIndex = -1

    @Volatile private var isMediaLoading = false

    @Volatile private var currentPositionMs = 0L

    @Volatile private var isPaused = false

    private var grabber: FFmpegFrameGrabber? = null
    private var audioLine: SourceDataLine? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playbackJob: Job? = null

    init {
        scope.launch {
            while (true) {
                delay(500)
                updateState()
            }
        }
    }

    private fun updateState() {
        val item = playlist.getOrNull(currentIndex)
        if (item == null) {
            _state.value = PlayerState.NoMedia
            return
        }
        val durationMs = grabber?.lengthInTime?.takeIf { it > 0L }?.div(1000L) ?: item.durationMs
        _state.value = PlayerState.MediaLoaded(
            isPlaying = !isPaused && !isMediaLoading,
            isLoading = isMediaLoading,
            showId = item.showId,
            showTitle = item.showTitle,
            durationInfo = MediaDurationInfo(currentPositionMs, durationMs),
            artworkUri = item.artworkUrl,
            title = item.title,
            albumTitle = item.albumTitle,
            mediaId = item.id,
            currentTrackIndex = currentIndex,
        )
    }

    private fun updateStateWithError(message: String) {
        val item = playlist.getOrNull(currentIndex) ?: return
        _state.value = PlayerState.MediaLoaded.Error(
            playerError = PlayerError(message),
            showId = item.showId,
            showTitle = item.showTitle,
            durationInfo = MediaDurationInfo(0L, item.durationMs),
            artworkUri = item.artworkUrl,
            title = item.title,
            albumTitle = item.albumTitle,
            mediaId = item.id,
            currentTrackIndex = currentIndex,
        )
    }

    override fun setPlaylist(items: List<PlaybackItem>, startIndex: Int) {
        playlist = items
        currentIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        val item = items.getOrNull(currentIndex) ?: return
        startPlayback(item)
    }

    private fun startPlayback(item: PlaybackItem, startPositionMs: Long = 0L) {
        val oldGrabber = grabber
        val oldLine = audioLine
        playbackJob?.cancel()
        grabber = null
        audioLine = null
        isMediaLoading = true
        isPaused = false
        currentPositionMs = startPositionMs
        updateState()
        playbackJob = scope.launch { runPlayback(item, startPositionMs, oldGrabber, oldLine) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun CoroutineScope.runPlayback(
        item: PlaybackItem,
        startPositionMs: Long,
        oldGrabber: FFmpegFrameGrabber?,
        oldLine: SourceDataLine?,
    ) {
        try {
            releaseResources(oldLine, oldGrabber)

            val newGrabber = FFmpegFrameGrabber(item.url).apply {
                sampleFormat = avutil.AV_SAMPLE_FMT_S16
                start()
            }
            if (startPositionMs > 0L) newGrabber.timestamp = startPositionMs * 1000L

            val sampleRate = newGrabber.sampleRate.takeIf { it > 0 } ?: 44100
            val channels = newGrabber.audioChannels.takeIf { it > 0 } ?: 2
            val audioFormat = AudioFormat(sampleRate.toFloat(), 16, channels, true, false)
            val lineInfo = DataLine.Info(SourceDataLine::class.java, audioFormat)
            val newLine = (AudioSystem.getLine(lineInfo) as SourceDataLine).apply {
                open(audioFormat)
                start()
            }

            grabber = newGrabber
            audioLine = newLine
            isMediaLoading = false
            updateState()

            runPlaybackLoop(newGrabber, newLine)

            if (isActive) {
                if (currentIndex < playlist.size - 1) skipToNext()
                else { isPaused = true; updateState() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Playback error for ${item.url}" }
            updateStateWithError("Playback error: ${e.message}")
        }
    }

    private suspend fun CoroutineScope.runPlaybackLoop(grabber: FFmpegFrameGrabber, line: SourceDataLine) {
        while (isActive) {
            if (!isPaused) {
                val frame = grabber.grabSamples() ?: break
                (frame.samples?.firstOrNull() as? ShortBuffer)?.let { writeAudioFrame(it, line) }
                currentPositionMs = grabber.timestamp / 1000L
            } else {
                delay(50)
            }
        }
    }

    private fun writeAudioFrame(buffer: ShortBuffer, line: SourceDataLine) {
        val buf = buffer.asReadOnlyBuffer()
        val bytes = ByteArray(buf.remaining() * 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(buf)
        line.write(bytes, 0, bytes.size)
    }

    private fun releaseResources(line: SourceDataLine?, grabber: FFmpegFrameGrabber?) {
        try { line?.stop(); line?.flush(); line?.close() } catch (_: Exception) {}
        try { grabber?.stop(); grabber?.release() } catch (_: Exception) {}
    }

    override fun play() {
        isPaused = false
        audioLine?.start()
        updateState()
    }

    override fun pause() {
        isPaused = true
        audioLine?.stop()
        updateState()
    }

    override fun seekTo(index: Int, positionMs: Long) {
        val targetIndex = index.coerceIn(0, (playlist.size - 1).coerceAtLeast(0))
        currentIndex = targetIndex
        val item = playlist.getOrNull(targetIndex) ?: return
        startPlayback(item, positionMs)
    }

    override fun skipToPrevious() {
        if (currentIndex > 0) seekTo(currentIndex - 1, 0L)
    }

    override fun skipToNext() {
        if (currentIndex < playlist.size - 1) seekTo(currentIndex + 1, 0L)
    }

    override fun release() {
        scope.cancel()
        audioLine?.stop()
        audioLine?.flush()
        audioLine?.close()
        grabber?.stop()
        grabber?.release()
    }
}
