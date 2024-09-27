package gizz.tapes.ui.show

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.Show
import gizz.tapes.api.data.ShowsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import gizz.tapes.ui.ApiErrorMessage
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ShowSelectionViewModel @Inject constructor(
    private val apiClient: GizzTapesApiClient,
    private val apiErrorMessage: ApiErrorMessage,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    val showYear: String = checkNotNull(savedStateHandle["year"])

    private val _shows: MutableStateFlow<LCE<List<ShowsData>, Throwable>> = MutableStateFlow(LCE.Loading)
    val shows: StateFlow<LCE<List<ShowsData>, Throwable>> = _shows

    init {
        loadShows()
    }

    private fun loadShows() {
        viewModelScope.launch {
            val state: LCE.Content<List<ShowsData>> = retryUntilSuccessful(
                action = {
                    apiClient.shows().map { shows ->
                        shows.filter { it.date.year.toString() == showYear }
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
