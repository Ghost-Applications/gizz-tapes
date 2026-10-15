package gizz.tapes.ui.settings

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.Platform
import gizz.tapes.api.data.Recording
import gizz.tapes.data.Settings
import gizz.tapes.isVolumeBoostSupported
import gizz.tapes.lastfm.LastFmAuthManager
import gizz.tapes.util.LC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class SettingsViewModel(
    private val dataStore: DataStore<Settings>,
    platform: Platform,
    private val lastFmAuthManager: LastFmAuthManager,
) : ViewModel() {

    private val isVolumeBoostSupported = platform.isVolumeBoostSupported

    val settingsState: StateFlow<LC<SettingsScreenState>> = loadSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = LC.Loading
    )

    private fun loadSettings(): Flow<LC<SettingsScreenState>> = flow {
        dataStore.data
            .combine(lastFmAuthManager.state) { settings, lastFmState ->
                SettingsScreenState(
                    selectedPreferredRecordingType = settings.preferredRecordingType,
                    volumeBoostEnabled = settings.volumeBoostEnabled,
                    isVolumeBoostSupported = isVolumeBoostSupported,
                    lastFmState = lastFmState,
                )
            }
            .map { LC.Content(it) }
            .collect { emit(it) }
    }

    fun updatePreferredRecordingType(type: Recording.Type) {
        viewModelScope.launch {
            dataStore.updateData { it.copy(preferredRecordingType = type) }
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
