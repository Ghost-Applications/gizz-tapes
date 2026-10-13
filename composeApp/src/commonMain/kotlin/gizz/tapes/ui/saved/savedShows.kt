package gizz.tapes.ui.saved

import co.touchlab.kermit.Logger
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.data.DownloadedShowData
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.ShowId
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.showTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val logger = Logger.withTag("SavedShows")

// only show ids are stored locally, so the show details come from the full show list - fetched
// once (HTTP cached) rather than once per show, then re-joined whenever the stored ids change.
internal fun savedShows(
    apiClient: GizzTapesApiClient,
    showIds: Flow<List<ShowId>>,
    arrange: (List<PartialShowData>) -> List<PartialShowData>,
): Flow<LCE<List<DownloadedShowData>, Exception>> = flow {
    val allShows = retryUntilSuccessful(
        action = { apiClient.shows() },
        onErrorAfter3SecondsAction = { error ->
            logger.d(error) { "Error retrieving shows" }
            emit(LCE.Error(error = error))
        }
    ).value
    val showsById = allShows.associateBy { ShowId(it.id) }

    emitAll(
        showIds.map { ids ->
            LCE.Content(arrange(ids.mapNotNull { showsById[it] }).map { it.toDownloadedShowData() })
        }
    )
}.flowOn(Dispatchers.IO)

private fun PartialShowData.toDownloadedShowData(): DownloadedShowData {
    val title = Title(showTitle)
    return DownloadedShowData(
        showId = ShowId(id),
        fullShowTitle = FullShowTitle(date = date, title = title),
        showTitle = title,
        showSubTitle = Subtitle(date),
        posterUrl = PosterUrl(posterUrl),
    )
}
