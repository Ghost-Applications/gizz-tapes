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
import gizz.tapes.data.MediaId
import gizz.tapes.stub
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

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

        data class ShowTestData(
            val mediaId: String,
            val mediaMetadataTitle: String,
            val albumArtwork: Uri?,
            val isPlayable: Boolean?,
            val isBrowsable: Boolean?,
            val mediaType: Int?
        )

        val classUnderTest = MediaItemTree(apiClient)

        val result = classUnderTest.getChildren(MediaId.RootId)
            .map {
                ShowTestData(
                    mediaId = it.mediaId,
                    mediaMetadataTitle = it.mediaMetadata.title.toString(),
                    albumArtwork = it.mediaMetadata.artworkUri,
                    isPlayable = it.mediaMetadata.isPlayable,
                    isBrowsable = it.mediaMetadata.isBrowsable,
                    mediaType = it.mediaMetadata.mediaType
                )
            }

        assertThat(result).containsExactly(
            ShowTestData(
                mediaId = "root/2019",
                mediaMetadataTitle = "2019",
                albumArtwork = Uri.parse("https://example.com/poster3.jpg"),
                isPlayable = false,
                isBrowsable = true,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_YEARS
            ),
            ShowTestData(
                mediaId = "root/2020",
                mediaMetadataTitle = "2020",
                albumArtwork = Uri.parse("https://example.com/poster2.jpg"),
                isPlayable = false,
                isBrowsable = true,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_YEARS
            ),
            ShowTestData(
                mediaId = "root/2021",
                mediaMetadataTitle = "2021",
                albumArtwork = Uri.parse("https://example.com/poster1.jpg"),
                isPlayable = false,
                isBrowsable = true,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_YEARS
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

        val classUnderTest = MediaItemTree(apiClient)

        data class ShowTestData(
            val mediaId: String,
            val mediaMetadataTitle: String,
            val mediaMetadataDisplayTitle: String,
            val artworkUri: Uri?,
            val isPlayable: Boolean?,
            val isBrowsable: Boolean?,
            val mediaType: Int?
        )

        val yearId = MediaId.YearId("2021")
        val result = classUnderTest.getChildren(yearId)
            .map {
                ShowTestData(
                    mediaId = it.mediaId,
                    mediaMetadataTitle = it.mediaMetadata.title.toString(),
                    mediaMetadataDisplayTitle = it.mediaMetadata.displayTitle.toString(),
                    artworkUri = it.mediaMetadata.artworkUri,
                    isPlayable = it.mediaMetadata.isPlayable,
                    isBrowsable = it.mediaMetadata.isBrowsable,
                    mediaType = it.mediaMetadata.mediaType
                )
            }

        assertThat(result).containsExactly(
            ShowTestData(
                mediaId = "root/2021/3",
                mediaMetadataTitle = "Venue 3 - Title 3 - Location 3",
                mediaMetadataDisplayTitle = "2021/12/10 Venue 3 - Title 3 - Location 3",
                artworkUri = Uri.parse("https://example.com/poster3.jpg"),
                isPlayable = false,
                isBrowsable = true,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
            ),
            ShowTestData(
                mediaId = "root/2021/2",
                mediaMetadataTitle = "Venue 2 - Title 2 - Location 2",
                mediaMetadataDisplayTitle = "2021/8/15 Venue 2 - Title 2 - Location 2",
                artworkUri = Uri.parse("https://example.com/poster2.jpg"),
                isPlayable = false,
                isBrowsable = true,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
            ),
            ShowTestData(
                mediaId = "root/2021/1",
                mediaMetadataTitle = "Venue 1 - Title 1 - Location 1",
                mediaMetadataDisplayTitle = "2021/5/20 Venue 1 - Title 1 - Location 1",
                artworkUri = Uri.parse("https://example.com/poster1.jpg"),
                isPlayable = false,
                isBrowsable = true,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
            )
        )
    }

    @Test
    fun `getChildren returns recording media for when parent id is show`() = runTest {
        val apiClient = object : GizzTapesApiClient {
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
                    )
                ).right()
            }

            override suspend fun show(id: String): Either<Exception, Show> {
                return Show(
                    id = id,
                    date = LocalDate(2021, 5, 20),
                    title = "Title 1",
                    posterUrl = "https://example.com/poster1.jpg",
                    order = 1U,
                    notes = "show notes",
                    kglwNet = KglwNet(
                        id = 10U,
                        permalink = "http://example.com"
                    ),
                    venueId = 10U,
                    tourId = 1U,
                    recordings = nonEmptyListOf(
                        Recording(
                            id = "kglw2024-11-20archie",
                            uploadedAt = Instant.fromEpochMilliseconds(2000),
                            type = Recording.Type.SBD,
                            source = null,
                            lineage = null,
                            taper = null,
                            files = nonEmptyListOf(
                                KglwFile(
                                    filename = "01-Intro.mp3",
                                    length = 10.seconds,
                                    title = "Intro"
                                ),
                                KglwFile(
                                    filename = "02-Rattlesnake.mp3",
                                    length = 629.seconds,
                                    title = "Rattlesnake"
                                ),
                                KglwFile(
                                    filename = "03-O.N.E..mp3",
                                    length = 229.seconds,
                                    title = "O.N.E."
                                )
                            ),
                            filesPathPrefix = "https://archive.org/download/kglw2024-11-20archie/",
                            internetArchive = InternetArchive(
                                isLma = true
                            )
                        )
                    )
                ).right()
            }

        }

        val classUnderTest = MediaItemTree(apiClient)

        data class TestData(
            val mediaId: String,
            val title: String,
            val displayTitle: String,
            val artist: String,
            val albumTitle: String,
            val albumArtist: String,
            val releaseYear: Int?,
            val releaseMonth: Int?,
            val releaseDay: Int?,
            val artworkUri: Uri,
            val isBrowsable: Boolean?,
            val isPlayable: Boolean?,
            val mediaType: Int?,
        )

        val showId = MediaId.ShowId(MediaId.YearId("2021"), "1")
        val result = classUnderTest.getChildren(showId)
            .map {
                TestData(
                    mediaId = it.mediaId,
                    title = it.mediaMetadata.title.toString(),
                    displayTitle = it.mediaMetadata.displayTitle.toString(),
                    artist = it.mediaMetadata.artist.toString(),
                    albumTitle = it.mediaMetadata.albumTitle.toString(),
                    artworkUri = checkNotNull(it.mediaMetadata.artworkUri),
                    isBrowsable = it.mediaMetadata.isBrowsable,
                    isPlayable = it.mediaMetadata.isPlayable,
                    releaseYear = it.mediaMetadata.releaseYear,
                    releaseMonth = it.mediaMetadata.releaseMonth,
                    releaseDay = it.mediaMetadata.releaseDay,
                    mediaType = it.mediaMetadata.mediaType,
                    albumArtist = it.mediaMetadata.albumArtist.toString(),
                )
            }

        assertThat(result).containsExactly(
            TestData(
                mediaId = "root/2021/1/kglw2024-11-20archie",
                title = "Title 1",
                artist = "2021/5/20 Venue 1 - Title 1 - Location 1",
                albumTitle = "Venue 1 - Title 1 - Location 1",
                albumArtist = "King Gizzard & The Lizard Wizard",
                artworkUri = Uri.parse("https://example.com/poster1.jpg"),
                isBrowsable = true,
                isPlayable = true,
                releaseYear = 2021,
                releaseMonth = 5,
                releaseDay = 20,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                displayTitle = "SBD: kglw2024-11-20archie "
            )
        )
    }

    @Test
    fun `getChildren returns track media for when parent id is recording`() = runTest {
        val apiClient = object : GizzTapesApiClient {
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
                    )
                ).right()
            }

            override suspend fun show(id: String): Either<Exception, Show> {
                return Show(
                    id = id,
                    date = LocalDate(2021, 5, 20),
                    title = "Title 1",
                    posterUrl = "https://example.com/poster1.jpg",
                    order = 1U,
                    notes = "show notes",
                    kglwNet = KglwNet(
                        id = 10U,
                        permalink = "http://example.com"
                    ),
                    venueId = 10U,
                    tourId = 1U,
                    recordings = nonEmptyListOf(
                        Recording(
                            id = "kglw2024-11-20archie",
                            uploadedAt = Instant.fromEpochMilliseconds(2000),
                            type = Recording.Type.SBD,
                            source = null,
                            lineage = null,
                            taper = null,
                            files = nonEmptyListOf(
                                KglwFile(
                                    filename = "01-Intro.mp3",
                                    length = 10.seconds,
                                    title = "Intro"
                                ),
                                KglwFile(
                                    filename = "02-Rattlesnake.mp3",
                                    length = 629.seconds,
                                    title = "Rattlesnake"
                                ),
                                KglwFile(
                                    filename = "03-O.N.E..mp3",
                                    length = 229.seconds,
                                    title = "O.N.E."
                                )
                            ),
                            filesPathPrefix = "https://archive.org/download/kglw2024-11-20archie/",
                            internetArchive = InternetArchive(
                                isLma = true
                            )
                        )
                    )
                ).right()
            }

        }

        val classUnderTest = MediaItemTree(apiClient)

        data class TestData(
            val mediaId: String,
            val title: String,
            val artist: String,
            val albumTitle: String,
            val albumArtist: String,
            val duration: Long?,
            val artworkUri: Uri,
            val isBrowsable: Boolean?,
            val isPlayable: Boolean?,
            val recordingYear: Int?,
            val recordingMonth: Int?,
            val recordingDay: Int?,
            val mediaType: Int?,
        )

        val showId = MediaId.RecordingId(
            parent = MediaId.ShowId(MediaId.YearId("2021"), "1"),
            recordingId = "kglw2024-11-20archie"
        )
        val result = classUnderTest.getChildren(showId)
            .map {
                TestData(
                    mediaId = it.mediaId,
                    title = it.mediaMetadata.title.toString(),
                    artist = it.mediaMetadata.artist.toString(),
                    albumTitle = it.mediaMetadata.albumTitle.toString(),
                    albumArtist = it.mediaMetadata.albumArtist.toString(),
                    duration = it.mediaMetadata.durationMs,
                    artworkUri = checkNotNull(it.mediaMetadata.artworkUri),
                    isBrowsable = it.mediaMetadata.isBrowsable,
                    isPlayable = it.mediaMetadata.isPlayable,
                    recordingYear = it.mediaMetadata.recordingYear,
                    recordingMonth = it.mediaMetadata.recordingMonth,
                    recordingDay = it.mediaMetadata.recordingDay,
                    mediaType = it.mediaMetadata.mediaType

                )
            }

        assertThat(result).containsExactly(
            TestData(
                mediaId = "root/2021/1/kglw2024-11-20archie/01-Intro.mp3",
                title = "Intro",
                artist = "2021/5/20 Title 1",
                albumTitle = "Title 1",
                albumArtist = "King Gizzard & The Lizard Wizard",
                duration = 10000L,
                artworkUri = Uri.parse("https://example.com/poster1.jpg"),
                isBrowsable = false,
                isPlayable = true,
                recordingYear = 2021,
                recordingMonth = 5,
                recordingDay = 20,
                mediaType = MediaMetadata.MEDIA_TYPE_MUSIC
            ),
            TestData(
                mediaId = "root/2021/1/kglw2024-11-20archie/02-Rattlesnake.mp3",
                title = "Rattlesnake",
                artist = "2021/5/20 Title 1",
                albumTitle = "Title 1",
                albumArtist = "King Gizzard & The Lizard Wizard",
                duration = 629000L,
                artworkUri = Uri.parse("https://example.com/poster1.jpg"),
                isBrowsable = false,
                isPlayable = true,
                recordingYear = 2021,
                recordingMonth = 5,
                recordingDay = 20,
                mediaType = MediaMetadata.MEDIA_TYPE_MUSIC
            ),
            TestData(
                mediaId = "root/2021/1/kglw2024-11-20archie/03-O.N.E..mp3",
                title = "O.N.E.",
                artist = "2021/5/20 Title 1",
                albumTitle = "Title 1",
                albumArtist = "King Gizzard & The Lizard Wizard",
                duration = 229000L,
                artworkUri = Uri.parse("https://example.com/poster1.jpg"),
                isBrowsable = false,
                isPlayable = true,
                recordingYear = 2021,
                recordingMonth = 5,
                recordingDay = 20,
                mediaType = MediaMetadata.MEDIA_TYPE_MUSIC
            )
        )
    }
} 
