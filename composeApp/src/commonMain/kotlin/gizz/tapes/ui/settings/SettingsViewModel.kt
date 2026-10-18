package gizz.tapes.ui.settings

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.Platform
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.Taper
import gizz.tapes.data.DEFAULT_RECORDING_TYPE_RANKING
import gizz.tapes.data.Settings
import gizz.tapes.isVolumeBoostSupported
import gizz.tapes.lastfm.LastFmAuthManager
import gizz.tapes.util.LC
import gizz.tapes.util.LCE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class SettingsViewModel(
    private val dataStore: DataStore<Settings>,
    private val apiClient: GizzTapesApiClient,
    platform: Platform,
    private val lastFmAuthManager: LastFmAuthManager,
) : ViewModel() {

    private val isVolumeBoostSupported = platform.isVolumeBoostSupported

    val settingsState: StateFlow<LC<SettingsScreenState>> = loadSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = LC.Loading
    )

    private fun loadSettings(): Flow<LC<SettingsScreenState>> =
        combine(dataStore.data, loadTapers(), lastFmAuthManager.state) { settings, tapers, lastFmState ->
            LC.Content(
                SettingsScreenState(
                    rankedRecordingTypes = settings.rankedRecordingTypes,
                    favouriteTaperIds = settings.favouriteTaperIds,
                    tapers = tapers,
                    volumeBoostEnabled = settings.volumeBoostEnabled,
                    isVolumeBoostSupported = isVolumeBoostSupported,
                    lastFmState = lastFmState,
                )
            )
        }

    private fun loadTapers(): Flow<LCE<List<TaperItem>, Throwable>> = flow {
        emit(LCE.Loading)
        emit(
            apiClient.tapers().fold(
                ifLeft = { LCE.Error(it) },
                ifRight = { tapers ->
                    dataStore.updateData { it.migrateFavouriteTapers(tapers) }
                    LCE.Content(tapers.toFavouriteCandidates())
                }
            )
        )
    }

    fun updateRecordingTypesRanked(ranked: Boolean) {
        viewModelScope.launch {
            dataStore.updateData {
                it.copy(rankedRecordingTypes = if (ranked) DEFAULT_RECORDING_TYPE_RANKING else null)
            }
        }
    }

    fun moveRecordingType(from: Int, to: Int) {
        viewModelScope.launch {
            dataStore.updateData { settings ->
                val ranking = settings.recordingTypeRanking.toMutableList()
                if (from !in ranking.indices || to !in ranking.indices) return@updateData settings
                ranking.add(to, ranking.removeAt(from))
                settings.copy(rankedRecordingTypes = ranking)
            }
        }
    }

    fun toggleFavouriteTaper(id: UInt) {
        viewModelScope.launch {
            dataStore.updateData {
                val favourites = it.favouriteTaperIds
                it.copy(favouriteTaperIds = if (id in favourites) favourites - id else favourites + id)
            }
        }
    }

    fun updateVolumeBoostEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.updateData { it.copy(volumeBoostEnabled = enabled) }
        }
    }

    fun connectLastFm(openUrl: (String) -> Unit) {
        viewModelScope.launch {
            lastFmAuthManager.startAuth()?.let(openUrl)
        }
    }

    fun cancelLastFm() = lastFmAuthManager.cancelAuth()

    fun disconnectLastFm() = lastFmAuthManager.logout()
}

/** Mirrors the taper filtering on tapes.kglw.net/settings. */
private fun List<Taper>.toFavouriteCandidates(): List<TaperItem> =
    asSequence()
        .filter { !it.isAnon && !it.name.isNullOrBlank() }
        .filterNot { it.name.orEmpty().contains("sam joseph", ignoreCase = true) }
        .filter { it.recordings.size > MIN_TAPER_RECORDINGS }
        .map { TaperItem(id = it.id, name = it.name.orEmpty(), uploadCount = it.recordings.size) }
        .sortedByDescending { it.uploadCount }
        .toList()

private const val MIN_TAPER_RECORDINGS = 2
