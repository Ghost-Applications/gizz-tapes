@file:Suppress("FunctionName", "FunctionNaming")

package gizz.tapes.playback

import gizz.tapes.ui.player.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.withContext
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.exceptions.DBusExecutionException
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MprisRoot : DBusInterface {
    @DBusMemberName("Raise")
    fun raise()

    @DBusMemberName("Quit")
    fun quit()
}

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface MprisPlayer : DBusInterface {
    @DBusMemberName("Next")
    fun next()

    @DBusMemberName("Previous")
    fun previous()

    @DBusMemberName("Pause")
    fun pause()

    @DBusMemberName("PlayPause")
    fun playPause()

    @DBusMemberName("Stop")
    fun stop()

    @DBusMemberName("Play")
    fun play()

    @DBusMemberName("Seek")
    fun seek(offset: Long)

    @DBusMemberName("SetPosition")
    fun setPosition(trackId: DBusPath, position: Long)

    @DBusMemberName("OpenUri")
    fun openUri(uri: String)

    class Seeked(path: String, val position: Long) : DBusSignal(path, position)
}

/**
 * Hooks the desktop player into Linux media keys and desktop "now playing" widgets by exposing it
 * as an MPRIS2 player (https://specifications.freedesktop.org/mpris-spec/latest/) on the session
 * D-Bus.
 */
class LinuxMpris(private val player: GizzMediaPlayer) : MprisRoot, MprisPlayer, Properties {

    @Volatile
    private var connection: DBusConnection? = null

    suspend fun run() = withContext(Dispatchers.IO) {
        DBusConnectionBuilder.forSessionBus().build().use { connection ->
            this@LinuxMpris.connection = connection
            connection.exportObject(OBJECT_PATH, this@LinuxMpris)
            connection.requestBusName(BUS_NAME)

            // Clients extrapolate Position from Rate, so only announce track/play-state changes.
            var lastMediaId: String? = null
            player.state
                .distinctUntilChangedBy { state ->
                    when (state) {
                        is PlayerState.MediaLoaded -> Triple(
                            state.mediaId,
                            state.isPlaying,
                            state.durationInfo.duration
                        )
                        PlayerState.NoMedia -> null
                    }
                }
                .collect { state ->
                    connection.sendMessage(
                        Properties.PropertiesChanged(OBJECT_PATH, PLAYER_INTERFACE, playerProperties(), emptyList())
                    )
                    // Seeking in the app reloads the same track, and Position changes are only
                    // announced through Seeked, so resync it whenever the play state changes.
                    val mediaId = (state as? PlayerState.MediaLoaded)?.mediaId
                    if (mediaId != null && mediaId == lastMediaId) {
                        connection.sendMessage(MprisPlayer.Seeked(OBJECT_PATH, player.currentPosition * 1000))
                    }
                    lastMediaId = mediaId
                }
        }
    }

    override fun getObjectPath() = OBJECT_PATH

    override fun raise() = Unit
    override fun quit() = Unit

    override fun next() = player.skipToNext()
    override fun previous() = player.skipToPrevious()
    override fun pause() = player.pause()
    override fun stop() = player.pause()
    override fun play() = player.play()
    override fun playPause() {
        if (loaded?.isPlaying == true) player.pause() else player.play()
    }

    override fun seek(offset: Long) {
        seekTo(player.currentPosition * 1000 + offset)
    }

    override fun setPosition(trackId: DBusPath, position: Long) {
        // Per spec, ignore requests for a track that is no longer current.
        if (trackId.path == loaded?.let(::trackId)?.path) seekTo(position)
    }

    override fun openUri(uri: String) = Unit

    private fun seekTo(positionUs: Long) {
        val item = loaded ?: return
        val positionMs = (positionUs / 1000).coerceIn(0, item.durationInfo.duration)
        player.seekTo(item.currentTrackIndex, positionMs)
        connection?.sendMessage(MprisPlayer.Seeked(OBJECT_PATH, positionMs * 1000))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <A : Any?> Get(interfaceName: String, propertyName: String): A =
        GetAll(interfaceName)[propertyName] as A
            ?: throw DBusExecutionException("Unknown property $interfaceName.$propertyName")

    override fun <A : Any?> Set(interfaceName: String, propertyName: String, value: A) = Unit

    override fun GetAll(interfaceName: String): Map<String, Variant<*>> = when (interfaceName) {
        ROOT_INTERFACE -> mapOf(
            "CanQuit" to Variant(false),
            "CanRaise" to Variant(false),
            "HasTrackList" to Variant(false),
            "Identity" to Variant("Gizz Tapes"),
            "SupportedUriSchemes" to Variant(emptyArray<String>()),
            "SupportedMimeTypes" to Variant(emptyArray<String>()),
        )
        PLAYER_INTERFACE -> playerProperties() + mapOf(
            "Position" to Variant(player.currentPosition * 1000),
        )
        else -> emptyMap()
    }

    private fun playerProperties(): Map<String, Variant<*>> {
        val item = loaded
        return mapOf(
            "PlaybackStatus" to Variant(
                when {
                    item == null -> "Stopped"
                    item.isPlaying -> "Playing"
                    else -> "Paused"
                }
            ),
            "Rate" to Variant(1.0),
            "MinimumRate" to Variant(1.0),
            "MaximumRate" to Variant(1.0),
            "Volume" to Variant(1.0),
            "Metadata" to Variant(metadata(item), "a{sv}"),
            "CanGoNext" to Variant(item != null),
            "CanGoPrevious" to Variant(item != null),
            "CanPlay" to Variant(item != null),
            "CanPause" to Variant(item != null),
            "CanSeek" to Variant(item != null),
            "CanControl" to Variant(true),
        )
    }

    private fun metadata(item: PlayerState.MediaLoaded?): Map<String, Variant<*>> {
        item ?: return emptyMap()
        return buildMap {
            put("mpris:trackid", Variant(trackId(item)))
            put("mpris:length", Variant(item.durationInfo.duration * 1000))
            put("xesam:title", Variant(item.title))
            put("xesam:album", Variant(item.albumTitle))
            // The bundled jar: fallback image isn't readable by other processes.
            item.artworkUri?.takeIf { it.startsWith("http") || it.startsWith("file:") }
                ?.let { put("mpris:artUrl", Variant(it)) }
        }
    }

    private val loaded get() = player.state.value as? PlayerState.MediaLoaded

    // Object paths only allow [A-Za-z0-9_], so the media id can't be used directly.
    private fun trackId(item: PlayerState.MediaLoaded) = DBusPath("/gizz/tapes/track/${item.currentTrackIndex}")

    private companion object {
        const val BUS_NAME = "org.mpris.MediaPlayer2.gizztapes"
        const val OBJECT_PATH = "/org/mpris/MediaPlayer2"
        const val ROOT_INTERFACE = "org.mpris.MediaPlayer2"
        const val PLAYER_INTERFACE = "org.mpris.MediaPlayer2.Player"
    }
}
