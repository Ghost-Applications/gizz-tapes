package gizz.tapes.storage

import gizz.tapes.AppContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MusicDownloaderTest {
    private val fileSystem = FileSystem.SYSTEM
    private lateinit var testHome: String
    private lateinit var appContext: AppContext

    @BeforeTest
    fun setUp() {
        val originalHome = System.getProperty("user.home")
        testHome = Files.createTempDirectory("music-downloader-test").toString()
        // AppContext.downloadsPath is computed once at construction from user.home, so this
        // property swap only needs to be in effect for the instant we build it.
        System.setProperty("user.home", testHome)
        appContext = AppContext()
        System.setProperty("user.home", originalHome)
    }

    @AfterTest
    fun tearDown() {
        fileSystem.deleteRecursively(testHome.toPath())
    }

    // MockEngine hops onto a real (non-virtual-time) dispatcher by default, which advanceUntilIdle()
    // can't see - pinning both the engine and the downloader's internal scope to the *same*
    // StandardTestDispatcher keeps everything, including Arrow's retry delay()s, on one virtual clock.
    private fun TestScope.downloader(handler: MockRequestHandler): MusicDownloader {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = MockEngine(MockEngineConfig().apply {
            addHandler(handler)
            this.dispatcher = dispatcher
        })
        return MusicDownloader(
            appContext = appContext,
            downloadHttpClient = DownloadHttpClient(HttpClient(engine)),
            fileSystem = fileSystem,
            dispatcher = dispatcher,
        )
    }

    @Test
    fun `download writes the file and reports SUCCEEDED`() = runTest {
        val downloader = downloader { respond(content = "audio-bytes", status = HttpStatusCode.OK) }

        downloader.download("https://example.com/track.mp3", "rec1/track.mp3")
        advanceUntilIdle()

        assertEquals(DownloadState.SUCCEEDED, downloader.downloadState("rec1/track.mp3").first())
        val destination = appContext.downloadsPath / "rec1/track.mp3"
        assertTrue(fileSystem.exists(destination))
        assertEquals("audio-bytes", fileSystem.read(destination) { readUtf8() })
    }

    @Test
    fun `download retries a failed request and eventually succeeds`() = runTest {
        var attempt = 0
        val downloader = downloader {
            attempt++
            if (attempt < 3) {
                respond(content = "", status = HttpStatusCode.InternalServerError)
            } else {
                respond(content = "audio-bytes", status = HttpStatusCode.OK)
            }
        }

        downloader.download("https://example.com/track.mp3", "rec1/track.mp3")
        advanceUntilIdle()

        assertEquals(3, attempt)
        assertEquals(DownloadState.SUCCEEDED, downloader.downloadState("rec1/track.mp3").first())
    }

    @Test
    fun `download reports FAILED once retries are exhausted, without leaving a partial file`() = runTest {
        val downloader = downloader { respond(content = "", status = HttpStatusCode.InternalServerError) }

        downloader.download("https://example.com/track.mp3", "rec1/track.mp3")
        advanceUntilIdle()

        assertEquals(DownloadState.FAILED, downloader.downloadState("rec1/track.mp3").first())
        assertFalse(fileSystem.exists(appContext.downloadsPath / "rec1/track.mp3"))
        assertFalse(fileSystem.exists(appContext.downloadsPath / "rec1/track.mp3.tmp"))
    }

    @Test
    fun `forgetFinishedDownloads resets a terminal state back to NOT_STARTED`() = runTest {
        val downloader = downloader { respond(content = "audio-bytes", status = HttpStatusCode.OK) }

        downloader.download("https://example.com/track.mp3", "rec1/track.mp3")
        advanceUntilIdle()
        assertEquals(DownloadState.SUCCEEDED, downloader.downloadState("rec1/track.mp3").first())

        downloader.forgetFinishedDownloads()

        assertEquals(DownloadState.NOT_STARTED, downloader.downloadState("rec1/track.mp3").first())
    }

    @Test
    fun `a second download for the same path while one is in flight is a no-op`() = runTest {
        var requestCount = 0
        val downloader = downloader {
            requestCount++
            respond(content = "audio-bytes", status = HttpStatusCode.OK)
        }

        downloader.download("https://example.com/track.mp3", "rec1/track.mp3")
        downloader.download("https://example.com/track.mp3", "rec1/track.mp3")
        advanceUntilIdle()

        assertEquals(1, requestCount)
    }
}
