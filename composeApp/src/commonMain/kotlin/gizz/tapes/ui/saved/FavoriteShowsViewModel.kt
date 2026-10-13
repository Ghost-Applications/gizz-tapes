package gizz.tapes.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.data.DownloadedShowData
import gizz.tapes.storage.SavedShowsRepository
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LCE
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class FavoriteShowsViewModel(
    apiClient: GizzTapesApiClient,
    savedShowsRepository: SavedShowsRepository,
) : ViewModel() {

    // newest shows first.
    val shows: StateFlow<LCE<List<DownloadedShowData>, Exception>> =
        savedShows(apiClient, savedShowsRepository.favoriteShowIds) { shows ->
            shows.sortedWith(
                compareByDescending<PartialShowData> { it.date }.thenByDescending { it.order }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.ForViewModel,
            initialValue = LCE.Loading
        )
}
