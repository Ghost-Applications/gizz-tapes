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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    private val _shows: MutableStateFlow<LCE<List<ShowSelectionData>, Throwable>> = MutableStateFlow(LCE.Loading)
    val shows: StateFlow<LCE<List<ShowSelectionData>, Throwable>> = _shows

    init {
        loadShows()
    }

    private fun loadShows() {
        viewModelScope.launch {
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
                    _shows.emit(
                        LCE.Error(
                            userDisplayedMessage = apiErrorMessage.value,
                            error = error
                        )
                    )
                }
            )
            _shows.emit(state)
        }
    }
}
