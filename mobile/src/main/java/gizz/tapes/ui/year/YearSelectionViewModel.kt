package gizz.tapes.ui.year

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.GizzTapesApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import gizz.tapes.ui.ApiErrorMessage
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import timber.log.Timber
import javax.inject.Inject

data class YearRenderModel(
    val year: String,
    val showCount: Int
)

@HiltViewModel
class YearSelectionViewModel @Inject constructor(
    private val apiClient: GizzTapesApiClient,
    private val apiErrorMessage: ApiErrorMessage
): ViewModel() {

    private val _years: MutableStateFlow<LCE<List<YearRenderModel>, Throwable>> =
        MutableStateFlow(LCE.Loading)
    val years: StateFlow<LCE<List<YearRenderModel>, Throwable>> = _years

    init {
        loadYears()
    }

    private fun loadYears() {
        viewModelScope.launch {
            val state = retryUntilSuccessful(
                action = { apiClient.shows().map {
                    it.groupBy { it.date.year }
                        .map { (year, shows) -> YearRenderModel(year.toString(), shows.count()) }
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
