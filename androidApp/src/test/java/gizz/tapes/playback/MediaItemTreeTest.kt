@file:OptIn(ExperimentalTime::class)

package gizz.tapes.playback

import android.net.Uri
import androidx.media3.common.MediaMetadata
import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.InternetArchive
import gizz.tapes.api.data.KglwFile
import gizz.tapes.api.data.KglwNet
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Show
import gizz.tapes.stub
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class MediaItemTreeTest {

    @Test
    fun `getChildren returns year media when parentId is root`() = runTest {
        val apiClient = object : GizzTapesApiClient by stub() {
            override suspend fun shows(): Either<Exception, List<PartialShowData>> {
                return listOf(
                    PartialShowData(
                        id = "1",
                        date = LocalDate(2021, 5, 20),
                        venueName = "Venue 1",
                        location = "Location 1",
                        title = "Title 1",
                        posterUrl = "https://example.com/poster1.jpg",
                        order = 1U
                    ),
                    PartialShowData(
                        id = "2",
                        date = LocalDate(2020, 8, 15),
                        venueName = "Venue 2",
                        location = "Location 2",
                        title = "Title 2",
                        posterUrl = "https://example.com/poster2.jpg",
                        order = 2U
                    ),
                    PartialShowData(
                        id = "3",
                        date = LocalDate(2019, 12, 10),
                        venueName = "Venue 3",
                        location = "Location 3",
                        title = "Title 3",
                        posterUrl = "https://example.com/poster3.jpg",
                        order = 3U
                    )
                ).right()
            }
        }

        data class ShowTestData(val mediaId: String, val title: String, val albumArtwork: Uri?, val isPlayable: Boolean?, val isBrowsable: Boolean?, val mediaType: Int?)

        val result = MediaItemTree(apiClient).getChildren(MediaId.RootId).map {
            ShowTestData(
                it.mediaId,
                it.mediaMetadata.title.toString(),
                it.mediaMetadata.artworkUri,
                it.mediaMetadata.isPlayable,
                it.mediaMetadata.isBrowsable,
                it.mediaMetadata.mediaType
            )
        }

        assertThat(result).containsExactly(
            ShowTestData(
                "root/2019",
                "2019",
                Uri.parse("https://example.com/poster3.jpg"),
                false,
                true,
                MediaMetadata.MEDIA_TYPE_FOLDER_YEARS
            ),
            ShowTestData(
                "root/2020",
                "2020",
                Uri.parse("https://example.com/poster2.jpg"),
                false,
                true,
                MediaMetadata.MEDIA_TYPE_FOLDER_YEARS
            ),
            ShowTestData(
                "root/2021",
                "2021",
                Uri.parse("https://example.com/poster1.jpg"),
                false,
                true,
                MediaMetadata.MEDIA_TYPE_FOLDER_YEARS
            )
        )
    }

    @Test
    fun `getChildren returns show media when parent id is year`() = runTest {
        val apiClient = object : GizzTapesApiClient by stub() {
            override suspend fun shows(): Either<Exception, List<PartialShowData>> {
                return listOf(
                    PartialShowData(
                        id = "1",
                        date = LocalDate(2021, 5, 20),
                        venueName = "Venue 1",
                        location = "Location 1",
                        title = "Title 1",
                        posterUrl = "https://example.com/poster1.jpg",
                        order = 1U
                    ),
                    PartialShowData(
                        id = "2",
                        date = LocalDate(2021, 8, 15),
                        venueName = "Venue 2",
                        location = "Location 2",
                        title = "Title 2",
                        posterUrl = "https://example.com/poster2.jpg",
                        order = 2U
                    ),
                    PartialShowData(
                        id = "3",
                        date = LocalDate(2021, 12, 10),
                        venueName = "Venue 3",
                        location = "Location 3",
                        title = "Title 3",
                        posterUrl = "https://example.com/poster3.jpg",
                        order = 3U
                    )
                ).right()
            }
        }

        data class ShowTestData(val mediaId: String, val title: String, val displayTitle: String, val artworkUri: Uri?, val isPlayable: Boolean?, val isBrowsable: Boolean?, val mediaType: Int?)

        val result = MediaItemTree(apiClient).getChildren(MediaId.YearId("2021")).map {
            ShowTestData(
                it.mediaId,
                it.mediaMetadata.title.toString(),
                it.mediaMetadata.displayTitle.toString(),
                it.mediaMetadata.artworkUri,
                it.mediaMetadata.isPlayable,
                it.mediaMetadata.isBrowsable,
                it.mediaMetadata.mediaType
            )
        }

        assertThat(result).containsExactly(
            ShowTestData(
                "root/2021/3",
                "Venue 3 - Title 3 - Location 3",
                "2021/12/10 Venue 3 - Title 3 - Location 3",
                Uri.parse("https://example.com/poster3.jpg"),
                false,
                true,
                MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
            ),
            ShowTestData(
                "root/2021/2",
                "Venue 2 - Title 2 - Location 2",
                "2021/8/15 Venue 2 - Title 2 - Location 2",
                Uri.parse("https://example.com/poster2.jpg"),
                false,
                true,
                MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
            ),
            ShowTestData(
                "root/2021/1",
                "Venue 1 - Title 1 - Location 1",
                "2021/5/20 Venue 1 - Title 1 - Location 1",
                Uri.parse("https://example.com/poster1.jpg"),
                false,
                true,
                MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
            )
        )
    }

    @Test
    fun `getChildren returns recording media for when parent id is show`() = runTest {
        val apiClient = object : GizzTapesApiClient {
            override suspend fun shows(): Either<Exception, List<PartialShowData>> = listOf(
                PartialShowData(
                    id = "1",
                    date = LocalDate(2021, 5, 20),
                    venueName = "Venue 1",
                    location = "Location 1",
                    title = "Title 1",
                    posterUrl = "https://example.com/poster1.jpg",
                    order = 1U
                )
            ).right()

            override suspend fun show(id: String): Either<Exception, Show> = Show(
                id = id, date = LocalDate(
                    2021,
                    5,
                    20
                ), title = "Title 1", posterUrl = "https://example.com/poster1.jpg",
                order = 1U, notes = "show notes", kglwNet = KglwNet(id = 10U, permalink = "http://example.com"),
                venueId = 10U, tourId = 1U,
                recordings = nonEmptyListOf(
                    Recording(
                        id = "kglw2024-11-20archie", uploadedAt = Instant.fromEpochMilliseconds(2000),
                        type = Recording.Type.SBD, source = null, lineage = null, taper = null,
                        files = nonEmptyListOf(
                            KglwFile("01-Intro.mp3", 10.seconds, "Intro"),
                            KglwFile("02-Rattlesnake.mp3", 629.seconds, "Rattlesnake"),
                            KglwFile("03-O.N.E..mp3", 229.seconds, "O.N.E.")
                        ),
                        filesPathPrefix = "https://archive.org/download/kglw2024-11-20archie/",
                        internetArchive = InternetArchive(isLma = true)
                    )
                )
            ).right()
        }

        data class TestData(val mediaId: String, val title: String, val displayTitle: String, val artist: String, val albumTitle: String, val albumArtist: String, val releaseYear: Int?, val releaseMonth: Int?, val releaseDay: Int?, val artworkUri: Uri?, val isBrowsable: Boolean?, val isPlayable: Boolean?, val mediaType: Int?)

        val result = MediaItemTree(apiClient)
            .getChildren(MediaId.ShowId(parent = MediaId.YearId("2021"), showId = "1"))
            .map {
                TestData(
                    it.mediaId, it.mediaMetadata.title.toString(), it.mediaMetadata.displayTitle.toString(),
                    it.mediaMetadata.artist.toString(), it.mediaMetadata.albumTitle.toString(), it.mediaMetadata.albumArtist.toString(),
                    it.mediaMetadata.releaseYear, it.mediaMetadata.releaseMonth, it.mediaMetadata.releaseDay,
                    it.mediaMetadata.artworkUri, it.mediaMetadata.isBrowsable, it.mediaMetadata.isPlayable, it.mediaMetadata.mediaType
                )
            }

        assertThat(result).containsExactly(
            TestData(
                "root/2021/1/kglw2024-11-20archie", "Title 1", "SBD: kglw2024-11-20archie ",
                "2021/5/20 Venue 1 - Title 1 - Location 1", "Venue 1 - Title 1 - Location 1", "King Gizzard & The Lizard Wizard",
                2021, 5, 20, Uri.parse(
                    "https://example.com/poster1.jpg"
                ), true, true, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
            )
        )
    }

    @Test
    fun `getChildren returns track media for when parent id is recording`() = runTest {
        val apiClient = object : GizzTapesApiClient {
            override suspend fun shows(): Either<Exception, List<PartialShowData>> = listOf(
                PartialShowData(
                    id = "1",
                    date = LocalDate(2021, 5, 20),
                    venueName = "Venue 1",
                    location = "Location 1",
                    title = "Title 1",
                    posterUrl = "https://example.com/poster1.jpg",
                    order = 1U
                )
            ).right()

            override suspend fun show(id: String): Either<Exception, Show> = Show(
                id = id, date = LocalDate(
                    2021,
                    5,
                    20
                ), title = "Title 1", posterUrl = "https://example.com/poster1.jpg",
                order = 1U, notes = "show notes", kglwNet = KglwNet(id = 10U, permalink = "http://example.com"),
                venueId = 10U, tourId = 1U,
                recordings = nonEmptyListOf(
                    Recording(
                        id = "kglw2024-11-20archie", uploadedAt = Instant.fromEpochMilliseconds(2000),
                        type = Recording.Type.SBD, source = null, lineage = null, taper = null,
                        files = nonEmptyListOf(
                            KglwFile("01-Intro.mp3", 10.seconds, "Intro"),
                            KglwFile("02-Rattlesnake.mp3", 629.seconds, "Rattlesnake"),
                            KglwFile("03-O.N.E..mp3", 229.seconds, "O.N.E.")
                        ),
                        filesPathPrefix = "https://archive.org/download/kglw2024-11-20archie/",
                        internetArchive = InternetArchive(isLma = true)
                    )
                )
            ).right()
        }

        data class TestData(val mediaId: String, val title: String, val artist: String, val albumTitle: String, val albumArtist: String, val duration: Long?, val artworkUri: Uri?, val isBrowsable: Boolean?, val isPlayable: Boolean?, val recordingYear: Int?, val recordingMonth: Int?, val recordingDay: Int?, val mediaType: Int?)

        val result = MediaItemTree(apiClient)
            .getChildren(
                MediaId.RecordingId(
                    parent = MediaId.ShowId(parent = MediaId.YearId("2021"), showId = "1"),
                    recordingId = "kglw2024-11-20archie"
                )
            )
            .map {
                TestData(
                    it.mediaId, it.mediaMetadata.title.toString(), it.mediaMetadata.artist.toString(),
                    it.mediaMetadata.albumTitle.toString(), it.mediaMetadata.albumArtist.toString(), it.mediaMetadata.durationMs,
                    it.mediaMetadata.artworkUri, it.mediaMetadata.isBrowsable, it.mediaMetadata.isPlayable,
                    it.mediaMetadata.recordingYear, it.mediaMetadata.recordingMonth, it.mediaMetadata.recordingDay, it.mediaMetadata.mediaType
                )
            }

        assertThat(result).containsExactly(
            TestData("root/2021/1/kglw2024-11-20archie/01-Intro.mp3", "Intro", "2021/5/20 Title 1", "Title 1", "King Gizzard & The Lizard Wizard", 10000L, Uri.parse(
                "https://example.com/poster1.jpg"
            ), false, true, 2021, 5, 20, MediaMetadata.MEDIA_TYPE_MUSIC),
            TestData("root/2021/1/kglw2024-11-20archie/02-Rattlesnake.mp3", "Rattlesnake", "2021/5/20 Title 1", "Title 1", "King Gizzard & The Lizard Wizard", 629000L, Uri.parse(
                "https://example.com/poster1.jpg"
            ), false, true, 2021, 5, 20, MediaMetadata.MEDIA_TYPE_MUSIC),
            TestData("root/2021/1/kglw2024-11-20archie/03-O.N.E..mp3", "O.N.E.", "2021/5/20 Title 1", "Title 1", "King Gizzard & The Lizard Wizard", 229000L, Uri.parse(
                "https://example.com/poster1.jpg"
            ), false, true, 2021, 5, 20, MediaMetadata.MEDIA_TYPE_MUSIC)
        )
    }
}
