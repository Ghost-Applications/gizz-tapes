package gizz.tapes.data

import gizz.tapes.storage.DownloadedShow

data class DownloadedShowData(
    val showId: ShowId,
    val fullShowTitle: FullShowTitle,
    val showTitle: Title,
    val showSubTitle: Subtitle,
    val posterUrl: PosterUrl,
) {
    companion object {
        operator fun invoke(downloaded: DownloadedShow): DownloadedShowData {
            val show = downloaded.show
            val title = downloaded.title
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
