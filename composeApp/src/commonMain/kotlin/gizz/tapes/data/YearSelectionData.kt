package gizz.tapes.data

import androidx.compose.runtime.Immutable

@Immutable
data class YearSelectionData(
    val year: Year,
    val showCount: Int,
    val randomShowPoster: PosterUrl,
)
