package gizz.tapes.data

import gizz.tapes.api.data.PartialShowData
import gizz.tapes.util.showTitle

data class ShowSelectionData(
    val showId: ShowId,
    val fullShowTitle: FullShowTitle,
    val showTitle: Title,
    val showSubTitle: Subtitle,
    val posterUrl: PosterUrl,
) {
    companion object {
        operator fun invoke(showData: PartialShowData): ShowSelectionData {
            val title = Title(showData.showTitle)
            return ShowSelectionData(
                showId = ShowId(showData.id),
                fullShowTitle = FullShowTitle(date = showData.date, title = title),
                showTitle = title,
                showSubTitle = Subtitle(showData.date),
                posterUrl = PosterUrl.Companion(showData.posterUrl)
            )
        }
    }
}
