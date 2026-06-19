package gizz.tapes.data

import gizz.tapes.api.data.Show

data class DownloadedShowData(
    val showId: ShowId,
    val fullShowTitle: FullShowTitle,
    val showTitle: Title,
    val showSubTitle: Subtitle,
    val posterUrl: PosterUrl,
) {
    companion object {
        operator fun invoke(show: Show): DownloadedShowData {
            val title = Title(show.title.orEmpty())
            return DownloadedShowData(
                showId = ShowId(show.id),
                fullShowTitle = FullShowTitle(date = show.date, title = title),
                showTitle = title,
                showSubTitle = Subtitle(show.date),
                posterUrl = PosterUrl(show.posterUrl),
            )
        }
    }
}
