package gizz.tapes.ui.show

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
import gizz.tapes.data.ShowId
import gizz.tapes.data.ShowSelectionData
import gizz.tapes.nav.Destination
import gizz.tapes.util.retryUntilSuccessful
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Resolves a show id from a deep link into the [FullShowTitle] that [Destination.Show] needs. The
 * title is built from the show list data (venue, location, artist), which the single show
 * endpoint doesn't include.
 */
@AssistedInject
class ShowLinkViewModel(
    private val apiClient: GizzTapesApiClient,
    @Assisted savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Error(val error: Throwable) : State
        data object NotFound : State
        data class Resolved(val id: ShowId, val title: FullShowTitle) : State
    }

    private val showId = savedStateHandle.toRoute<Destination.ShowLink>(Destination.ShowLink.typeMap).id
    private val logger = Logger.withTag("ShowLinkViewModel")

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val shows = retryUntilSuccessful(
                action = { apiClient.shows() },
                onErrorAfter3SecondsAction = { error ->
                    logger.d(error) { "Error retrieving shows" }
                    _state.value = State.Error(error)
                }
            ).value

            _state.value = shows.firstOrNull { it.id == showId.value }
                ?.let { State.Resolved(showId, ShowSelectionData(it).fullShowTitle) }
                ?: State.NotFound
        }
    }

    @AssistedFactory
    @ViewModelAssistedFactoryKey(ShowLinkViewModel::class)
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ViewModelAssistedFactory {
        fun create(savedStateHandle: SavedStateHandle): ShowLinkViewModel
        override fun create(extras: CreationExtras): ShowLinkViewModel = create(extras.createSavedStateHandle())
    }
}
