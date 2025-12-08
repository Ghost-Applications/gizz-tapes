package gizz.tapes.ui.years

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gizz.tapes.AppGraph
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Settings
import gizz.tapes.data.SortOrder
import gizz.tapes.data.Year
import gizz.tapes.data.YearSelectionData
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class YearSelectionViewModel(
    private val apiClient: GizzTapesApiClient,
    private val apiErrorMessage: ApiErrorMessage,
    private val settingsDataStore: DataStore<Settings>
) : ViewModel() {

    val sortOrder: StateFlow<SortOrder> = settingsDataStore.data
        .map { it.yearSortOrder }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SortOrder.Ascending
        )

    val years = loadYears().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
        initialValue = LCE.Loading
    )

    fun updateSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            settingsDataStore.updateData {
                it.copy(yearSortOrder = sortOrder)
            }
        }
    }

    private fun loadYears(): Flow<LCE<List<YearSelectionData>, Throwable>> {
        return flow {
            val state = retryUntilSuccessful(
                action = {
                    apiClient.shows().map {
                        it.groupBy { show -> show.date.year }
                            .map { (year, shows) ->
                                YearSelectionData(
                                    year = Year(year),
                                    showCount = shows.count(),
                                    randomShowPoster = PosterUrl(shows.random().posterUrl)
                                )
                            }
                            .reversed()
                    }
                },
                onErrorAfter3SecondsAction = { error ->
                    emit(
                        LCE.Error(
                            userDisplayedMessage = "There was an error", // TODO apiErrorMessage.value,
                            error = error
                        )
                    )
                }
            )

            emit(state)
        }
    }
}

@Composable
fun YearSelectionScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Cyan)) {
        Text("It works!")
    }
}
