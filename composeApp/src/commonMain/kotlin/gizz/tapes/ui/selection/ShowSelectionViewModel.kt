package gizz.tapes.ui.selection

import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Settings
import gizz.tapes.data.ShowId
import gizz.tapes.data.ShowSelectionData
import gizz.tapes.data.SortOrder
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.data.Year
import gizz.tapes.nav.Destination
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.showTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AssistedInject
class ShowSelectionViewModel(
    private val apiClient: GizzTapesApiClient,
    private val apiErrorMessage: ApiErrorMessage,
    private val settingsDataStore: DataStore<Settings>,
    @Assisted savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val logger = Logger.withTag("ShowSelectionViewModel")

    val showYear: Year = savedStateHandle
        .toRoute<Destination.ShowSelection>(Destination.ShowSelection.typeMap)
        .year

    val shows: StateFlow<LCE<List<ShowSelectionData>, Throwable>> = loadShows().stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LCE.Loading
    )

    val sortOrder: StateFlow<SortOrder> = settingsDataStore.data
        .map { it.showSortOrder }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.ForViewModel,
            initialValue = SortOrder.Ascending
        )

    fun updateSortOrder(order: SortOrder) {
        viewModelScope.launch {
            settingsDataStore.updateData { it.copy(showSortOrder = order) }
        }
    }

    private fun loadShows(): Flow<LCE<List<ShowSelectionData>, Throwable>> = flow {
        val state = retryUntilSuccessful(
            action = {
                apiClient.shows().map { shows ->
                    shows.filter { it.date.year.toString() == showYear.value }
                        .map { show ->
                            val title = Title(show.showTitle)
                            ShowSelectionData(
                                showId = ShowId(show.id),
                                fullShowTitle = FullShowTitle(date = show.date, title = title),
                                showTitle = title,
                                showSubTitle = Subtitle.Companion(show.date),
                                posterUrl = PosterUrl.Companion(show.posterUrl)
                            )
                        }
                }
            },
            onErrorAfter3SecondsAction = { error ->
                logger.d(error) { "Error retrieving shows" }
                emit(LCE.Error(userDisplayedMessage = apiErrorMessage.value, error = error))
            }
        )
        emit(state)
    }

    @AssistedFactory
    @ViewModelAssistedFactoryKey(ShowSelectionViewModel::class)
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ViewModelAssistedFactory {
        fun create(savedStateHandle: SavedStateHandle): ShowSelectionViewModel
        override fun create(extras: CreationExtras): ShowSelectionViewModel = create(extras.createSavedStateHandle())
    }
}
