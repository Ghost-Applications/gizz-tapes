@file:Suppress("FunctionName", "FunctionNaming")

package gizz.tapes.playback

import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.Structure
import gizz.tapes.ui.player.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Hooks the desktop player into macOS media keys, Control Center and the menu bar "Now Playing"
 * widget via MPRemoteCommandCenter/MPNowPlayingInfoCenter. There's no JVM API for these, so this
 * talks to the Objective-C runtime directly through JNA, mirroring what IosMediaPlayer does
 * natively.
 */
class MacNowPlaying(private val player: GizzMediaPlayer) {

    // objc_msgSend isn't variadic on arm64, so every call shape needs its own fixed-arity binding.
    @Suppress("TooManyFunctions")
    private interface ObjC : Library {
        fun objc_getClass(name: String): Pointer
        fun sel_registerName(name: String): Pointer
        fun objc_allocateClassPair(superclass: Pointer, name: String, extraBytes: Long): Pointer
        fun class_addMethod(cls: Pointer, sel: Pointer, imp: Callback, types: String): Byte
        fun objc_registerClassPair(cls: Pointer)
        fun objc_autoreleasePoolPush(): Pointer
        fun objc_autoreleasePoolPop(pool: Pointer)
        fun objc_msgSend(receiver: Pointer, sel: Pointer): Pointer?
        fun objc_msgSend(receiver: Pointer, sel: Pointer, arg: Pointer?): Pointer?
        fun objc_msgSend(receiver: Pointer, sel: Pointer, arg1: Pointer, arg2: Pointer?): Pointer?
        fun objc_msgSend(receiver: Pointer, sel: Pointer, arg: String): Pointer?
        fun objc_msgSend(receiver: Pointer, sel: Pointer, arg: Double): Pointer?
        fun objc_msgSend(receiver: Pointer, sel: Pointer, arg: Long): Pointer?
        fun objc_msgSend(receiver: Pointer, sel: Pointer, bytes: ByteArray, length: Long): Pointer?
        fun objc_msgSend(
            receiver: Pointer,
            sel: Pointer,
            width: Double,
            height: Double,
            block: Pointer
        ): Pointer?
    }

    // separate interface since it can't overload the Pointer-returning objc_msgSend(receiver, sel)
    private interface ObjCDouble : Library {
        fun objc_msgSend(receiver: Pointer, sel: Pointer): Double
    }

    private interface ObjCSize : Library {
        fun objc_msgSend(receiver: Pointer, sel: Pointer): CGSize
    }

    @Structure.FieldOrder("width", "height")
    class CGSize : Structure(), Structure.ByValue {
        @JvmField
        var width = 0.0

        @JvmField
        var height = 0.0
    }

    /** `- (MPRemoteCommandHandlerStatus)handle:(MPRemoteCommandEvent *)event` */
    private fun interface CommandHandler : Callback {
        fun invoke(self: Pointer?, cmd: Pointer?, event: Pointer?): Long
    }

    private val options = mapOf(Library.OPTION_STRING_ENCODING to "UTF-8")
    private val objc = Native.load("objc", ObjC::class.java, options)
    private val objcDouble = Native.load("objc", ObjCDouble::class.java, options)
    private val objcSize = Native.load("objc", ObjCSize::class.java, options)
    private val mediaPlayer = NativeLibrary.getInstance(
        "/System/Library/Frameworks/MediaPlayer.framework/MediaPlayer"
    )

    private val nowPlayingCenter = send(cls("MPNowPlayingInfoCenter"), "defaultCenter")!!

    // JNA frees a callback's native trampoline once it's garbage collected and MPRemoteCommand
    // doesn't retain its target, so both are held here for the lifetime of the process.
    private val handlers = mutableListOf<CommandHandler>()
    private var target: Pointer? = null

    /** `^NSImage *(CGSize size)` - a CGSize of two doubles is passed as two float registers. */
    private fun interface ArtworkHandler : Callback {
        fun invoke(block: Pointer?, width: Double, height: Double): Pointer?
    }

    // MPMediaItemArtwork on macOS can only be built from a block, which JNA can't create, so this
    // hand-rolls a static global block (see the Blocks ABI: isa, flags, reserved, invoke,
    // descriptor). It lives for the process and always returns whatever artwork is current.
    // Images are never released since an older MPMediaItemArtwork may still ask for them.
    @Volatile
    private var artworkImage: Pointer? = null
    private var artwork: Pointer? = null
    private var lastArtworkUrl: String? = null
    private val artworkHandler = ArtworkHandler { _, _, _ -> artworkImage }
    private val artworkBlockDescriptor = Memory(16).apply {
        setLong(0, 0) // reserved
        setLong(8, BLOCK_LITERAL_SIZE)
    }
    private val artworkBlock = Memory(BLOCK_LITERAL_SIZE).apply {
        setPointer(
            0,
            NativeLibrary.getInstance("System").getGlobalVariableAddress("_NSConcreteGlobalBlock")
        )
        setInt(8, BLOCK_IS_GLOBAL)
        setInt(12, 0)
        setPointer(16, CallbackReference.getFunctionPointer(artworkHandler))
        setPointer(24, artworkBlockDescriptor)
    }

    fun registerRemoteCommands() {
        val commands = mapOf(
            "playCommand" to CommandHandler { _, _, _ -> player.play(); SUCCESS },
            "pauseCommand" to CommandHandler { _, _, _ -> player.pause(); SUCCESS },
            "togglePlayPauseCommand" to CommandHandler { _, _, _ ->
                if ((player.state.value as? PlayerState.MediaLoaded)?.isPlaying == true) player.pause() else player.play()
                SUCCESS
            },
            "nextTrackCommand" to CommandHandler { _, _, _ -> player.skipToNext(); SUCCESS },
            "previousTrackCommand" to CommandHandler { _, _, _ -> player.skipToPrevious(); SUCCESS },
            "changePlaybackPositionCommand" to CommandHandler { _, _, event ->
                val positionSeconds = objcDouble.objc_msgSend(event!!, sel("positionTime"))
                val index = (player.state.value as? PlayerState.MediaLoaded)?.currentTrackIndex ?: 0
                player.seekTo(index, (positionSeconds * 1000.0).toLong())
                SUCCESS
            },
        )

        val targetClass = objc.objc_allocateClassPair(cls("NSObject"), "GizzRemoteCommandTarget", 0)
        commands.forEach { (command, handler) ->
            objc.class_addMethod(targetClass, sel("$command:"), handler, "q@:@")
        }
        objc.objc_registerClassPair(targetClass)
        handlers += commands.values

        val target = send(send(targetClass, "alloc")!!, "init")!!
        this.target = target
        val commandCenter = send(cls("MPRemoteCommandCenter"), "sharedCommandCenter")!!
        commands.keys.forEach { command ->
            val remoteCommand = send(commandCenter, command)!!
            objc.objc_msgSend(remoteCommand, sel("addTarget:action:"), target, sel("$command:"))
        }
    }

    suspend fun syncNowPlayingInfo() = coroutineScope {
        // Position ticks every 500ms but macOS extrapolates elapsed time from the playback rate,
        // so only push on track/play-state changes (seeking passes through Loading, so it counts).
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
                when (state) {
                    is PlayerState.MediaLoaded -> state.artworkUri?.let {
                        if (it != lastArtworkUrl) {
                            lastArtworkUrl = it
                            artwork?.let { artwork -> send(artwork, "release") }
                            artwork = null
                            launch {
                                // URI handles both remote posters and the bundled jar: fallback image
                                val bytes = withContext(Dispatchers.IO) {
                                    runCatching { URI(it).toURL().readBytes() }.getOrNull()
                                }
                                if (bytes != null && it == lastArtworkUrl) {
                                    setArtwork(bytes)
                                    updateNowPlayingInfo(player.state.value)
                                }
                            }
                        }
                    }
                    PlayerState.NoMedia -> Unit // no-op
                }
                updateNowPlayingInfo(state)
            }
    }

    private fun setArtwork(bytes: ByteArray) = withAutoreleasePool {
        val data = objc.objc_msgSend(
            cls("NSData"),
            sel("dataWithBytes:length:"),
            bytes,
            bytes.size.toLong()
        )!!
        val image = objc.objc_msgSend(send(cls("NSImage"), "alloc")!!, sel("initWithData:"), data)
            ?: return@withAutoreleasePool
        val size = objcSize.objc_msgSend(image, sel("size"))
        artworkImage = image
        artwork = objc.objc_msgSend(
            send(cls("MPMediaItemArtwork"), "alloc")!!,
            sel("initWithBoundsSize:requestHandler:"),
            size.width,
            size.height,
            artworkBlock,
        )
    }

    private fun updateNowPlayingInfo(state: PlayerState) = withAutoreleasePool {
        val item = state as? PlayerState.MediaLoaded
        if (item == null) {
            objc.objc_msgSend(nowPlayingCenter, sel("setNowPlayingInfo:"), null as Pointer?)
            objc.objc_msgSend(nowPlayingCenter, sel("setPlaybackState:"), PLAYBACK_STATE_STOPPED)
            return@withAutoreleasePool
        }

        val info = send(cls("NSMutableDictionary"), "dictionary")!!
        fun put(key: String, value: Pointer?) {
            val keyString = mediaPlayer.getGlobalVariableAddress(key).getPointer(0)
            objc.objc_msgSend(info, sel("setObject:forKey:"), value!!, keyString)
        }
        put("MPMediaItemPropertyTitle", nsString(item.title))
        put("MPMediaItemPropertyAlbumTitle", nsString(item.albumTitle))
        put("MPMediaItemPropertyPlaybackDuration", nsNumber(item.durationInfo.duration / 1000.0))
        put(
            "MPNowPlayingInfoPropertyElapsedPlaybackTime",
            nsNumber(item.durationInfo.currentPosition / 1000.0)
        )
        put("MPNowPlayingInfoPropertyPlaybackRate", nsNumber(if (item.isPlaying) 1.0 else 0.0))
        artwork?.let { put("MPMediaItemPropertyArtwork", it) }

        objc.objc_msgSend(nowPlayingCenter, sel("setNowPlayingInfo:"), info)
        // Per MPNowPlayingInfoCenter.h this must be set whenever playback starts or stops on macOS,
        // otherwise remote commands may not be routed to this app.
        objc.objc_msgSend(
            nowPlayingCenter,
            sel("setPlaybackState:"),
            if (item.isPlaying) PLAYBACK_STATE_PLAYING else PLAYBACK_STATE_PAUSED,
        )
    }

    private inline fun withAutoreleasePool(block: () -> Unit) {
        val pool = objc.objc_autoreleasePoolPush()
        try {
            block()
        } finally {
            objc.objc_autoreleasePoolPop(pool)
        }
    }

    private fun cls(name: String) = objc.objc_getClass(name)
    private fun sel(name: String) = objc.sel_registerName(name)
    private fun send(receiver: Pointer, selector: String) =
        objc.objc_msgSend(receiver, sel(selector))

    private fun nsString(value: String) =
        objc.objc_msgSend(cls("NSString"), sel("stringWithUTF8String:"), value)

    private fun nsNumber(value: Double) =
        objc.objc_msgSend(cls("NSNumber"), sel("numberWithDouble:"), value)

    private companion object {
        const val SUCCESS = 0L // MPRemoteCommandHandlerStatusSuccess
        const val PLAYBACK_STATE_PLAYING = 1L
        const val PLAYBACK_STATE_PAUSED = 2L
        const val PLAYBACK_STATE_STOPPED = 3L
        const val BLOCK_IS_GLOBAL = 1 shl 28
        const val BLOCK_LITERAL_SIZE = 32L
    }
}
