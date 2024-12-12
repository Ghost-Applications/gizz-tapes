package gizz.tapes.ui.year

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Year
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class YearSelectionViewModel @Inject constructor(
    private val apiClient: GizzTapesApiClient,
    private val apiErrorMessage: ApiErrorMessage
): ViewModel() {

    val years = loadYears().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LCE.Loading
    )

    private fun loadYears(): Flow<LCE<List<YearSelectionData>, Throwable>> {
        return flow {
            val state = retryUntilSuccessful(
                action = { apiClient.shows().map {
                    it.groupBy { show -> show.date.year }
                        .map { (year, shows) ->
                            YearSelectionData(
                                year = Year(year),
                                showCount = shows.count(),
                                randomShowPoster = PosterUrl(shows.random().posterUrl)
                            )
                        }
                        .reversed()
                } },
                onErrorAfter3SecondsAction = { error ->
                    Timber.d(error, "Error loading years.")
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
