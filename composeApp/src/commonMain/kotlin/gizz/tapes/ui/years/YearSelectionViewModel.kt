package gizz.tapes.ui.years

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Settings
import gizz.tapes.data.SortOrder
import gizz.tapes.data.Year
import gizz.tapes.data.YearSelectionData
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class YearSelectionViewModel(
    private val apiClient: GizzTapesApiClient,
    private val settingsDataStore: DataStore<Settings>
) : ViewModel() {

    private val logger = Logger.withTag("YearSelectionViewModel")

    val sortOrder: StateFlow<SortOrder> = settingsDataStore.data
        .map { it.yearSortOrder }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SortOrder.Ascending
        )

    val years = loadYears().stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LCE.Loading
    )

    fun updateSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            settingsDataStore.updateData { it.copy(yearSortOrder = sortOrder) }
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
                    logger.d(error) { "Error loading years." }
                    emit(
                        LCE.Error(error = error)
                    )
                }
            )
            emit(state)
        }
    }
}
