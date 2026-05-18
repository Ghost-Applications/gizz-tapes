package gizz.tapes.ui.venue

import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title

data class VenueSelectionData(
    val title: Title,
    val subtitle: Subtitle,
    val posterUrl: PosterUrl
)
