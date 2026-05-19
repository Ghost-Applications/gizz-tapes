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
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Settings
import gizz.tapes.data.ShowId
import gizz.tapes.data.ShowSelectionData
import gizz.tapes.data.SortOrder
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.nav.Destination
import gizz.tapes.nav.Destination.ShowSelection.SelectionType
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.map
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.showTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AssistedInject
class ShowSelectionViewModel(
    private val apiClient: GizzTapesApiClient,
    private val settingsDataStore: DataStore<Settings>,
    @Assisted savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val logger = Logger.withTag("ShowSelectionViewModel")

    private val sortOrder: StateFlow<SortOrder> = settingsDataStore.data
        .map { it.showSortOrder }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.ForViewModel,
            initialValue = SortOrder.Descending
        )

    private val showsFlow: Flow<LCE<List<ShowSelectionData>, Exception>> = flow {
        val state = retryUntilSuccessful(
            action = {
                apiClient.shows().map { shows ->
                    shows.filter {
                        when (selectionType) {
                            is SelectionType.ByVenue -> it.venueName == selectionType.venueName
                            is SelectionType.ByYear -> {
                                it.date.year.toString() == selectionType.year.toString()
                            }
                        }
                    }.map { show ->
                        val title = Title(show.showTitle)
                        ShowSelectionData(
                            showId = ShowId(show.id),
                            fullShowTitle = FullShowTitle(date = show.date, title = title),
                            showTitle = title,
                            showSubTitle = Subtitle(show.date),
                            posterUrl = PosterUrl(show.posterUrl)
                        )
                    }
                }
            },
            onErrorAfter3SecondsAction = { error ->
                logger.d(error) { "Error retrieving shows" }
                emit(LCE.Error(error = error))
            }
        )
        emit(state)
    }

    val selectionType: SelectionType = savedStateHandle
        .toRoute<Destination.ShowSelection>(Destination.ShowSelection.typeMap)
        .selectionType

    val shows: StateFlow<LCE<List<ShowSelectionData>, Exception>> = combine(
        showsFlow,
        sortOrder
    ) { showsLce, sortOrder ->
        showsLce.map { shows ->
            when (sortOrder) {
                SortOrder.Ascending -> when (selectionType) {
                    is SelectionType.ByVenue -> shows.sortedBy { it.showTitle.value }
                    is SelectionType.ByYear -> shows.sortedBy { it.fullShowTitle.date }
                }

                SortOrder.Descending -> when (selectionType) {
                    is SelectionType.ByVenue -> shows.sortedByDescending { it.showTitle.value }
                    is SelectionType.ByYear -> shows.sortedByDescending { it.fullShowTitle.date }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LCE.Loading
    )

    fun toggleSortOrder() {
        viewModelScope.launch {
            settingsDataStore.updateData { it.copy(showSortOrder = !sortOrder.value) }
        }
    }

    @AssistedFactory
    @ViewModelAssistedFactoryKey(ShowSelectionViewModel::class)
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ViewModelAssistedFactory {
        fun create(savedStateHandle: SavedStateHandle): ShowSelectionViewModel
        override fun create(extras: CreationExtras): ShowSelectionViewModel =
            create(extras.createSavedStateHandle())
    }
}
