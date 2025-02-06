package gizz.tapes.ui.selection

import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Settings
import gizz.tapes.data.ShowId
import gizz.tapes.data.SortOrder
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.data.Year
import gizz.tapes.ui.nav.ShowSelection
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.showTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val settingsDataStore: DataStore<Settings>,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    val showYear: Year = savedStateHandle.toRoute<ShowSelection>(ShowSelection.typeMap).year
    val shows = loadShows().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LCE.Loading
    )
    val sortOrder = settingsDataStore.data
        .map { it.showSortOrder }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SortOrder.Ascending
        )

    fun updateSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            settingsDataStore.updateData {
                it.copy(showSortOrder = sortOrder)
            }
        }
    }

    private fun loadShows(): Flow<LCE<List<ShowSelectionData>, Throwable>> {
        return flow {
            val state = retryUntilSuccessful(
                action = {
                    apiClient.shows().map { shows ->
                        shows.filter { it.date.year.toString() == showYear.value }
                            .map { show ->

                                val showTitle = Title(show.showTitle)

                                ShowSelectionData(
                                    showId = ShowId(show.id),
                                    fullShowTitle = FullShowTitle(date = show.date, title = showTitle),
                                    showTitle = showTitle,
                                    showSubTitle = Subtitle(show.date),
                                    posterUrl = PosterUrl(show.posterUrl)
                                )
                            }
                    }
                },
                onErrorAfter3SecondsAction = { error ->
                    Timber.d(error, "Error retrieving shows")
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
