package gizz.tapes.ui.home

import arrow.core.NonEmptyList
import gizz.tapes.data.ShowSelectionData
import gizz.tapes.util.LC
import gizz.tapes.util.LCE
import kotlinx.datetime.LocalDate

data class HomeScreenData(
    val header: LCE<HomeHeader, Exception>,
    val content: LCE<HomeContent, Exception>,
)

data class HomeHeader(
    val heroPhoto: String,
    val heroPhotoAttribution: String,
    val shows: Int,
    val recordings: Int,
    val hours: Int
)

data class HomeContent(
    val todayInGizztory: Gizztory?,
    val latestShows: NonEmptyList<ShowSelectionData>,
    val randomShows: LC<NonEmptyList<ShowSelectionData>>
)

data class Gizztory(
    val todayDate: LocalDate,
    val shows: NonEmptyList<ShowSelectionData>
)
