package gizz.tapes.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.data.DownloadedShowData
import gizz.tapes.storage.DownloadedShowsSource
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LCE
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class DownloadedShowsViewModel(
    private val downloadedShowsSource: DownloadedShowsSource,
) : ViewModel() {

    val shows: StateFlow<LCE<List<DownloadedShowData>, Exception>> = flow {
        emit(LCE.Content(downloadedShowsSource.loadDownloadedShows().map { DownloadedShowData(it) }))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LCE.Loading
    )
}
