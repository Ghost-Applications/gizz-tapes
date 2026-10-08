@file:Suppress("FunctionName", "FunctionNaming")

package gizz.tapes.playback

import co.touchlab.kermit.Logger
import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.PointerByReference
import gizz.tapes.ui.player.PlayerState
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Hooks the desktop player into Windows media keys and the volume/lock screen media overlay via
 * SystemMediaTransportControls. SMTC is a WinRT API with no JVM binding, so like [MacNowPlaying]
 * this drives it through JNA, calling COM vtable slots directly (indices per windows.media.idl).
 */
class WindowsMediaControls(private val player: GizzMediaPlayer, hwnd: Long) {

    private interface Combase : Library {
        fun RoInitialize(initType: Int): Int
        fun RoGetActivationFactory(classId: Pointer, iid: Memory, factory: PointerByReference): Int
        fun WindowsCreateString(source: WString, length: Int, string: PointerByReference): Int
        fun WindowsDeleteString(string: Pointer?): Int
    }

    private fun interface QueryInterface : Callback {
        fun invoke(self: Pointer, iid: Pointer, out: Pointer): Int
    }

    private fun interface RefCount : Callback {
        fun invoke(self: Pointer): Int
    }

    /** `TypedEventHandler<SystemMediaTransportControls, ButtonPressedEventArgs>::Invoke` */
    private fun interface ButtonPressed : Callback {
        fun invoke(self: Pointer, sender: Pointer?, args: Pointer): Int
    }

    private val logger = Logger.withTag("WindowsMediaControls")

    private val combase = Native.load("combase", Combase::class.java)

    // All WinRT calls happen on one MTA thread, the AWT threads may already be in an STA.
    private val comThread = Executors.newSingleThreadExecutor { Thread(it, "smtc").apply { isDaemon = true } }
        .asCoroutineDispatcher()

    private lateinit var controls: Pointer
    private lateinit var displayUpdater: Pointer
    private var lastArtworkUrl: String? = null

    // A static COM object implementing the ButtonPressed delegate. AddRef/Release are no-ops since
    // it lives for the process, and the callbacks are held here so JNA doesn't free them.
    private val queryInterface = QueryInterface { self, iid, out ->
        val requested = iid.getByteArray(0, GUID_SIZE)
        if (DELEGATE_IIDS.any { it.contentEquals(requested) }) {
            out.setPointer(0, self)
            S_OK
        } else {
            out.setPointer(0, null)
            E_NOINTERFACE
        }
    }
    private val refCount = RefCount { 1 }
    private val buttonPressed = ButtonPressed { _, _, args ->
        val button = Memory(4)
        if (args.call(EVENT_ARGS_GET_BUTTON, button) == S_OK) {
            when (button.getInt(0)) {
                BUTTON_PLAY -> player.play()
                BUTTON_PAUSE -> player.pause()
                BUTTON_NEXT -> player.skipToNext()
                BUTTON_PREVIOUS -> player.skipToPrevious()
            }
        }
        S_OK
    }
    private val handlerVtable = Memory(4L * Native.POINTER_SIZE).apply {
        listOf(queryInterface, refCount, refCount, buttonPressed).forEachIndexed { i, callback ->
            setPointer(i.toLong() * Native.POINTER_SIZE, CallbackReference.getFunctionPointer(callback))
        }
    }
    private val handler = Memory(Native.POINTER_SIZE.toLong()).apply { setPointer(0, handlerVtable) }

    private val hwnd = Pointer(hwnd)

    suspend fun run() = withContext(comThread) {
        combase.RoInitialize(RO_INIT_MULTITHREADED)

        val interop = activationFactory("Windows.Media.SystemMediaTransportControls", IID_INTEROP)
        controls = out { interop.call(INTEROP_GET_FOR_WINDOW, hwnd, guid(IID_SMTC), it) }
        interop.release()

        listOf(
            SMTC_PUT_IS_PLAY_ENABLED,
            SMTC_PUT_IS_PAUSE_ENABLED,
            SMTC_PUT_IS_NEXT_ENABLED,
            SMTC_PUT_IS_PREVIOUS_ENABLED
        ).forEach { controls.call(it, TRUE) }
        controls.call(SMTC_ADD_BUTTON_PRESSED, handler, Memory(8))
        displayUpdater = out { controls.call(SMTC_GET_DISPLAY_UPDATER, it) }

        player.state
            .distinctUntilChangedBy { state ->
                when (state) {
                    is PlayerState.MediaLoaded -> state.mediaId to state.isPlaying
                    PlayerState.NoMedia -> null
                }
            }
            .collect { state ->
                // Keep the controls alive if one update fails, e.g. on an artwork URL WinRT rejects.
                runCatching { update(state) }.onFailure { logger.w(it) { "SMTC update failed" } }
            }
    }

    private fun update(state: PlayerState) {
        val item = state as? PlayerState.MediaLoaded
        if (item == null) {
            controls.call(SMTC_PUT_IS_ENABLED, FALSE)
            controls.call(SMTC_PUT_PLAYBACK_STATUS, STATUS_CLOSED)
            return
        }

        controls.call(SMTC_PUT_IS_ENABLED, TRUE)
        controls.call(SMTC_PUT_PLAYBACK_STATUS, if (item.isPlaying) STATUS_PLAYING else STATUS_PAUSED)

        displayUpdater.call(UPDATER_PUT_TYPE, PLAYBACK_TYPE_MUSIC)
        val music = out { displayUpdater.call(UPDATER_GET_MUSIC_PROPERTIES, it) }
        withHString(item.title) { music.call(MUSIC_PUT_TITLE, it) }
        val music2 = out { music.call(QUERY_INTERFACE, guid(IID_MUSIC_PROPERTIES_2), it) }
        withHString(item.albumTitle) { music2.call(MUSIC2_PUT_ALBUM_TITLE, it) }
        music2.release()
        music.release()

        // RandomAccessStreamReference can't read the bundled jar: fallback image, so only remote art.
        val artworkUrl = item.artworkUri?.takeIf { it.startsWith("http") }
        if (artworkUrl != lastArtworkUrl) {
            lastArtworkUrl = artworkUrl
            val thumbnail = artworkUrl?.let(::streamReference)
            displayUpdater.call(UPDATER_PUT_THUMBNAIL, thumbnail)
            thumbnail?.release()
        }

        displayUpdater.call(UPDATER_UPDATE)
    }

    private fun streamReference(url: String): Pointer {
        val uriFactory = activationFactory("Windows.Foundation.Uri", IID_URI_FACTORY)
        val uri = withHString(url) { hstring -> out { uriFactory.call(URI_FACTORY_CREATE_URI, hstring, it) } }
        uriFactory.release()
        val statics = activationFactory("Windows.Storage.Streams.RandomAccessStreamReference", IID_STREAM_REF_STATICS)
        val reference = out { statics.call(STREAM_REF_CREATE_FROM_URI, uri, it) }
        statics.release()
        uri.release()
        return reference
    }

    private fun activationFactory(className: String, iid: String) = withHString(className) { name ->
        PointerByReference().also { checkHResult(combase.RoGetActivationFactory(name, guid(iid), it), className) }.value
    }

    private inline fun <T> withHString(value: String, block: (Pointer) -> T): T {
        val string = PointerByReference()
        checkHResult(combase.WindowsCreateString(WString(value), value.length, string), "WindowsCreateString")
        return try {
            block(string.value)
        } finally {
            combase.WindowsDeleteString(string.value)
        }
    }

    /** Calls a COM method whose last argument is an out pointer, returning what it wrote there. */
    private inline fun out(call: (PointerByReference) -> Int): Pointer {
        val result = PointerByReference()
        checkHResult(call(result), "COM call")
        return result.value
    }

    private fun Pointer.call(index: Int, vararg args: Any?): Int {
        val function = getPointer(0).getPointer(index.toLong() * Native.POINTER_SIZE)
        return Function.getFunction(function).invokeInt(arrayOf(this, *args))
    }

    private fun Pointer.release() = call(RELEASE)

    private fun checkHResult(hresult: Int, what: String) {
        check(hresult >= 0) { "$what failed: 0x${hresult.toUInt().toString(16)}" }
    }

    private companion object {
        const val S_OK = 0
        const val E_NOINTERFACE = 0x80004002.toInt()
        const val RO_INIT_MULTITHREADED = 1
        const val GUID_SIZE = 16
        const val TRUE: Byte = 1
        const val FALSE: Byte = 0

        const val QUERY_INTERFACE = 0
        const val RELEASE = 2

        const val INTEROP_GET_FOR_WINDOW = 6
        const val SMTC_PUT_PLAYBACK_STATUS = 7
        const val SMTC_GET_DISPLAY_UPDATER = 8
        const val SMTC_PUT_IS_ENABLED = 11
        const val SMTC_PUT_IS_PLAY_ENABLED = 13
        const val SMTC_PUT_IS_PAUSE_ENABLED = 17
        const val SMTC_PUT_IS_PREVIOUS_ENABLED = 25
        const val SMTC_PUT_IS_NEXT_ENABLED = 27
        const val SMTC_ADD_BUTTON_PRESSED = 32
        const val UPDATER_PUT_TYPE = 7
        const val UPDATER_PUT_THUMBNAIL = 11
        const val UPDATER_GET_MUSIC_PROPERTIES = 12
        const val UPDATER_UPDATE = 17
        const val MUSIC_PUT_TITLE = 7
        const val MUSIC2_PUT_ALBUM_TITLE = 7
        const val EVENT_ARGS_GET_BUTTON = 6
        const val URI_FACTORY_CREATE_URI = 6
        const val STREAM_REF_CREATE_FROM_URI = 7

        const val STATUS_CLOSED = 0
        const val STATUS_PLAYING = 3
        const val STATUS_PAUSED = 4
        const val PLAYBACK_TYPE_MUSIC = 1
        const val BUTTON_PLAY = 0
        const val BUTTON_PAUSE = 1
        const val BUTTON_NEXT = 6
        const val BUTTON_PREVIOUS = 7

        const val IID_INTEROP = "ddb0472d-c911-4a1f-86d9-dc3d71a95f5a"
        const val IID_SMTC = "99fa3ff4-1742-42a6-902e-087d41f965ec"
        const val IID_MUSIC_PROPERTIES_2 = "00368462-97d3-44b9-b00f-008afcefaf18"
        const val IID_URI_FACTORY = "44a9796f-723e-4fdf-a218-033e75b0c084"
        const val IID_STREAM_REF_STATICS = "857309dc-3fbf-4e7d-986f-ef3b1a07a964"

        // IUnknown, IAgileObject and TypedEventHandler<SystemMediaTransportControls, ButtonPressedEventArgs>
        val DELEGATE_IIDS = listOf(
            "00000000-0000-0000-c000-000000000046",
            "94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90",
            "0557e996-7b23-5bae-aa81-ea0d671143a4",
        ).map(::guidBytes)

        fun guid(value: String) = Memory(GUID_SIZE.toLong()).apply { write(0, guidBytes(value), 0, GUID_SIZE) }
    }
}

/** GUID layout: little-endian Data1/Data2/Data3, then Data4 bytes as-is. */
internal fun guidBytes(value: String): ByteArray {
    val uuid = UUID.fromString(value)
    val high = uuid.mostSignificantBits
    return ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        .putInt((high ushr 32).toInt())
        .putShort((high ushr 16).toShort())
        .putShort(high.toShort())
        .order(ByteOrder.BIG_ENDIAN)
        .putLong(uuid.leastSignificantBits)
        .array()
}
