package gizz.tapes.data

import androidx.compose.runtime.Immutable
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Year

@Immutable
data class YearSelectionData(
    val year: Year,
    val showCount: Int,
    val randomShowPoster: PosterUrl,
)
