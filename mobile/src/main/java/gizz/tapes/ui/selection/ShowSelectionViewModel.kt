package gizz.tapes.ui.selection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.ShowId
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.showTitle
import gizz.tapes.util.toSimpleFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

data class ShowSelectionData(
    val showId: ShowId,
    val fullShowTitle: FullShowTitle,
    val showTitle: Title,
    val showSubTitle: Subtitle,
    val posterUrl: PosterUrl,
)

@HiltViewModel
class ShowSelectionViewModel @Inject constructor(
    private val apiClient: GizzTapesApiClient,
    private val apiErrorMessage: ApiErrorMessage,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    val showYear: String = checkNotNull(savedStateHandle["year"])
    val shows = loadShows().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LCE.Loading
    )


    private fun loadShows(): Flow<LCE<List<ShowSelectionData>, Throwable>> {
        return flow {
            val state = retryUntilSuccessful(
                action = {
                    apiClient.shows().map { shows ->
                        shows.filter { it.date.year.toString() == showYear }
                            .map { show ->

                                val showTitle = Title(show.showTitle)

                                ShowSelectionData(
                                    showId = ShowId(show.id),
                                    fullShowTitle = FullShowTitle(show.date, showTitle),
                                    showTitle = showTitle,
                                    showSubTitle = Subtitle(show.date.toSimpleFormat()),
                                    posterUrl = PosterUrl(show.posterUrl)
                                )
                            }
                    }
                },
                onErrorAfter3SecondsAction = { error ->
                    Timber.d(error, "Error retrieving shows")
                    emit(
                        LCE.Error(
                            userDisplayedMessage = apiErrorMessage.value,
                            error = error
                        )
                    )
                }
            )

            emit(state)
        }
    }
}
