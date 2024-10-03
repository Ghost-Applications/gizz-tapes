package gizz.tapes.ui.year

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.GizzTapesApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import gizz.tapes.ui.ApiErrorMessage
import gizz.tapes.ui.data.PosterUrl
import gizz.tapes.ui.data.Year
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import timber.log.Timber
import javax.inject.Inject

data class YearSelectionData(
    val year: Year,
    val showCount: Int,
    val randomShowPoster: PosterUrl?,
)

@HiltViewModel
class YearSelectionViewModel @Inject constructor(
    private val apiClient: GizzTapesApiClient,
    private val apiErrorMessage: ApiErrorMessage
): ViewModel() {

    private val _years: MutableStateFlow<LCE<List<YearSelectionData>, Throwable>> =
        MutableStateFlow(LCE.Loading)
    val years: StateFlow<LCE<List<YearSelectionData>, Throwable>> = _years

    init {
        loadYears()
    }

    private fun loadYears() {
        viewModelScope.launch {
            val state = retryUntilSuccessful(
                action = { apiClient.shows().map {
                    it.groupBy { show -> show.date.year }
                        .map { (year, shows) ->
                            YearSelectionData(
                                year = Year(year),
                                showCount = shows.count(),
                                randomShowPoster = shows.random()
                                    .posterUrl?.let { url -> PosterUrl(url) }
                            )
                        }
                        .reversed()
                } },
                onErrorAfter3SecondsAction = { error ->
                    Timber.d(error, "Error loading years.")
                    _years.emit(
                        LCE.Error(
                            userDisplayedMessage = apiErrorMessage.value,
                            error = error
                        )
                    )
                }
            )

            _years.emit(state)
        }
    }
}
