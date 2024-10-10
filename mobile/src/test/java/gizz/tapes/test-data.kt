package gizz.tapes

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import arrow.core.nonEmptyListOf
import gizz.tapes.api.data.KglwFile
import gizz.tapes.api.data.InternetArchive
import gizz.tapes.api.data.KglwNet
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Show
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.ShowId
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.data.Year
import gizz.tapes.ui.player.MediaDurationInfo
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.show.ShowScreenData
import gizz.tapes.ui.show.ShowScreenData.Track
import gizz.tapes.ui.show.ShowSelectionData
import gizz.tapes.ui.show.TrackDuration
import gizz.tapes.ui.show.TrackId
import gizz.tapes.ui.show.TrackTitle
import gizz.tapes.ui.year.YearSelectionData
import gizz.tapes.util.LCE
import gizz.tapes.util.map
import gizz.tapes.util.showTitle
import gizz.tapes.util.toSimpleFormat
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val mediaItem = MediaItem.Builder()
    .setUri("https://archive.org/download/kglw2024-09-11.bandcampbootlegger/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 01 The Dripping Tap (Live).mp3")
    .setMediaId("King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 01 The Dripping Tap (Live).mp3")
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle("The Dripping Tap (Live)")
            .setAlbumTitle("Lake Tahoe Outdoor Arena at Harveys")
            .build()
    )
    .build()

val noShowPlayerState = PlayerState.NoMedia

val showingPlayerState = PlayerState.MediaLoaded(
    isPlaying = true,
    durationInfo = MediaDurationInfo(
        currentPosition = 1.minutes.inWholeMilliseconds,
        duration = 7.minutes.inWholeMilliseconds
    ),
    showId = ShowId("showId"),
    artworkUri = "https://kglw.net/i/poster-art-1699403482.jpeg".toUri(),
    albumTitle = "2024-09-11 : Edgefield Amphitheater - Troutdale, OR, USA",
    title = "Free",
    mediaId = "https://phish.in/audio/000/032/562/32562.mp3",
    showTitle = Title("Early Show - ")
)

val yearData = LCE.Content(
    value = listOf(
        YearSelectionData(
            year = Year("2024"),
            showCount = 32,
            randomShowPoster = PosterUrl("https://kglw.net/i/poster-art-1699403231.jpeg")
        ),
        YearSelectionData(
            year = Year("2023"),
            showCount = 10,
            randomShowPoster = PosterUrl("https://kglw.net/i/poster-art-1699403282.jpeg")
        ),
        YearSelectionData(
            year = Year("2022"),
            showCount = 5,
            randomShowPoster = PosterUrl(null)
        ),
        YearSelectionData(
            year = Year("2021"),
            showCount = 32,
            randomShowPoster = PosterUrl(null)
        ),
        YearSelectionData(
            year = Year("2020"),
            showCount = 32,
            randomShowPoster = PosterUrl(null)
        )
    )
)

val partialShowData = LCE.Content(
    value = listOf(
        PartialShowData(
            id = "2024-09-03",
            date = LocalDate.parse("2024-09-03"),
            venueName = "The Armory",
            location = "Minneapolis, MN, USA",
            title = "",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403231.jpeg"
        ),
        PartialShowData(
            id = "2024-09-04",
            date = LocalDate.parse("2024-09-04"),
            venueName = "Miller High Life Theatre",
            location = "Milwaukee, WI, USA",
            title = "",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403282.jpeg"
        ),
        PartialShowData(
            id = "2024-09-05",
            date = LocalDate.parse("2024-09-05"),
            venueName = "The Factory",
            location = "St. Louis, MO, USA",
            title = "",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403317.jpeg"
        ),
        PartialShowData(
            id = "2024-09-06",
            date = LocalDate.parse("2024-09-06"),
            venueName = "The Astro Amphitheater",
            location = "Omaha, NE, USA",
            title = "",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403370.jpeg"
        ),
        PartialShowData(
            id = "2024-09-08",
            date = LocalDate.parse("2024-09-08"),
            venueName = "Red Rocks Amphitheatre",
            location = "Morrison, CO, USA",
            title = "",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403394.jpeg"
        ),
        PartialShowData(
            id = "2024-09-09early",
            date = LocalDate.parse("2024-09-09"),
            venueName = "Red Rocks Amphitheatre",
            location = "Morrison, CO, USA",
            title = "Early Show",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403422.png"
        ),
        PartialShowData(
            id = "2024-09-09late",
            date = LocalDate.parse("2024-09-09"),
            venueName = "Red Rocks Amphitheatre",
            location = "Morrison, CO, USA",
            title = "Late Show",
            order = 2.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403442.jpeg"
        ),
        PartialShowData(
            id = "2024-09-11",
            date = LocalDate.parse("2024-09-11"),
            venueName = "Edgefield Amphitheater",
            location = "Troutdale, OR, USA",
            title = "",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403482.jpeg"
        ),
        PartialShowData(
            id = "2024-09-12",
            date = LocalDate.parse("2024-09-12"),
            venueName = "Pacific Coliseum",
            location = "Vancouver, BC, Canada",
            title = "",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403518.jpeg"
        ),
        PartialShowData(
            id = "2024-09-14",
            date = LocalDate.parse("2024-09-14"),
            venueName = "The Gorge Amphitheatre",
            location = "Quincy, WA, USA",
            title = "Marathon Show",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1694538149.jpeg"
        )
    )
)


val show = Show(
    id = "2024-09-11",
    date = LocalDate.parse("2024-09-11"),
    order = 1.toUShort(),
    posterUrl = "https://kglw.net/i/poster-art-1699403482.jpeg",
    notes = null,
    title = "",
    kglwNet = KglwNet(
        id = 1699403482.toUInt(),
        permalink = "king-gizzard-the-lizard-wizard-september-11-2024-edgefield-amphitheater-troutdale-or-usa.html"
    ),
    venueId = 726.toUInt(),
    tourId = 52.toUInt(),
    recordings = nonEmptyListOf(
        Recording(
            id = "kglw2024-09-11.bandcampbootlegger",
            uploadedAt = Instant.parse("2024-09-15T20:37:30+00:00"),
            type = Recording.Type.SBD,
            source = "SDB",
            lineage = "SBD > Bandcamp",
            taper = "Sam Joseph",
            filesPathPrefix = "https://archive.org/download/kglw2024-09-11.bandcampbootlegger/",
            internetArchive = InternetArchive(
                isLma = true
            ),
            files = nonEmptyListOf(
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 01 The Dripping Tap (Live).mp3",
                    length = 961.seconds,
                    title = "The Dripping Tap (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 02 Boogieman Sam (Live).mp3",
                    length = 484.seconds,
                    title = "Boogieman Sam (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 03 Daily Blues (Live).mp3",
                    length = 605.seconds,
                    title = "Daily Blues (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 04 Le Risque (Live).mp3",
                    length = 288.seconds,
                    title = "Le Risque (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 05 Crumbling Castle (Live).mp3",
                    length = 585.seconds,
                    title = "Crumbling Castle (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 06 The Fourth Colour (Live).mp3",
                    length = 321.seconds,
                    title = "The Fourth Colour (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 07 Self-Immolate (Live).mp3",
                    length = 435.seconds,
                    title = "Self-Immolate (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 08 Supercell (Live).mp3",
                    length = 305.seconds,
                    title = "Supercell (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 09 Organ Farmer (Live).mp3",
                    length = 216.seconds,
                    title = "Organ Farmer (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 10 Converge (Live).mp3",
                    length = 370.seconds,
                    title = "Converge (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 11 Witchcraft (Live).mp3",
                    length = 410.seconds,
                    title = "Witchcraft (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 12 Sad Pilot (Live).mp3",
                    length = 363.seconds,
                    title = "Sad Pilot (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 13 The Reticent Raconteur (Live).mp3",
                    length = 59.seconds,
                    title = "The Reticent Raconteur (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 14 The Lord of Lightning (Live).mp3",
                    length = 312.seconds,
                    title = "The Lord of Lightning (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 15 The Balrog (Live).mp3",
                    length = 213.seconds,
                    title = "The Balrog (Live)"
                ),
                KglwFile(
                    filename = "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 16 Field of Vision (Live).mp3",
                    length = 310.seconds,
                    title = "Field of Vision (Live)"
                )
            )
        )
    )
)

val showContent = LCE.Content(
    ShowScreenData(
        removeOldMediaItemsAndAddNew = { },
        showPosterUrl = PosterUrl("https://kglw.net/i/poster-art-1699403482.jpeg"),
        tracks = show.recordings.first().files.map {
            Track(
                id = TrackId(it.filename),
                title = TrackTitle(it.title),
                duration = TrackDuration(it.length)
            )
        }
    )
)

val showListContent = partialShowData.map { showData ->
    showData.map { data ->
        ShowSelectionData(
            showTitle = Title(data.showTitle),
            showId = ShowId(data.id),
            fullShowTitle = FullShowTitle(data.date, Title(data.showTitle)),
            showSubTitle = Subtitle(data.date),
            posterUrl = PosterUrl(data.posterUrl)
        )
    }
}