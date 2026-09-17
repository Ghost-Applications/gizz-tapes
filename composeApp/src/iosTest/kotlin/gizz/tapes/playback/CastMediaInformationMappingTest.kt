package gizz.tapes.playback

import cocoapods.google_cast_sdk.GCKImage
import cocoapods.google_cast_sdk.kGCKMetadataKeyAlbumTitle
import cocoapods.google_cast_sdk.kGCKMetadataKeyTitle
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalForeignApi::class)
class CastMediaInformationMappingTest {

    private val item = PlaybackItem(
        id = "recording-1/01-song.mp3",
        url = "/local/downloads/01-song.mp3",
        remoteUrl = "https://tapes.kglw.net/recording-1/01-song.mp3",
        title = "Song One",
        albumTitle = "A Show",
        artworkUrl = "https://tapes.kglw.net/poster.jpg",
        showId = ShowId("show-1"),
        showTitle = FullShowTitle(title = Title("A Show"), date = LocalDate(2024, 3, 15)),
        durationMs = 125_000L,
        showDate = LocalDate(2024, 3, 15),
    )

    @Test
    fun `toCastMediaInformation always uses remoteUrl not the local download path`() {
        val info = item.toCastMediaInformation()

        assertEquals(item.remoteUrl, info.contentID())
    }

    @Test
    fun `toCastMediaInformation round-trips recording date and showId through customData`() {
        val info = item.toCastMediaInformation()

        @Suppress("UNCHECKED_CAST")
        val customData = info.customData() as Map<Any?, Any?>
        assertEquals(2024L, (customData["recordingYear"] as Number).toLong())
        assertEquals(3L, (customData["recordingMonth"] as Number).toLong())
        assertEquals(15L, (customData["recordingDay"] as Number).toLong())
        assertEquals("show-1", customData["showId"])
    }

    @Test
    fun `toCastMediaInformation carries title album title and duration`() {
        val info = item.toCastMediaInformation()

        assertEquals(item.title, info.metadata?.stringForKey(kGCKMetadataKeyTitle))
        assertEquals(item.albumTitle, info.metadata?.stringForKey(kGCKMetadataKeyAlbumTitle))
        assertEquals(item.durationMs / 1000.0, info.streamDuration())
    }

    @Test
    fun `toCastMediaInformation includes the poster image when artworkUrl is present`() {
        val info = item.toCastMediaInformation()

        val images = info.metadata?.images().orEmpty()
        assertEquals(1, images.size)
        assertEquals(item.artworkUrl, (images.first() as GCKImage).URL.absoluteString)
    }

    @Test
    fun `toCastMediaInformation adds no image when artworkUrl is null`() {
        val info = item.copy(artworkUrl = null).toCastMediaInformation()

        assertEquals(0, info.metadata?.images().orEmpty().size)
    }
}
