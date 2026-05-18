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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
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

    private val sortOrder: StateFlow<SortOrder> = settingsDataStore.data
        .map { it.yearSortOrder }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SortOrder.Descending
        )

    val years = loadSortedYears().stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LCE.Loading
    )

    fun toggleSortOrder() {
        viewModelScope.launch {
            settingsDataStore.updateData { it.copy(yearSortOrder = !sortOrder.value) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadSortedYears(): Flow<LCE<List<YearSelectionData>, Exception>> {
        return settingsDataStore.data
            .map { it.yearSortOrder }
            .flatMapLatest {
                loadYears(it)
            }
    }

    private fun loadYears(sortOrder: SortOrder): Flow<LCE<List<YearSelectionData>, Exception>> {
        return flow {
            val years = retryUntilSuccessful(
                action = {
                    apiClient.years().map { years ->
                        if (sortOrder == SortOrder.Ascending) {
                            years.sortedBy { it.year }
                        } else {
                            years.sortedByDescending { it.year }
                        }.map {
                            YearSelectionData(
                                year = Year(it.year),
                                showCount = it.showCount,
                                poster = PosterUrl(it.posterUrl)
                            )
                        }
                    }
                },
                onErrorAfter3SecondsAction = { error ->
                    logger.d(error) { "Error loading years." }
                    emit(
                        LCE.Error(error)
                    )
                }
            )
            emit(years)
        }
    }
}
