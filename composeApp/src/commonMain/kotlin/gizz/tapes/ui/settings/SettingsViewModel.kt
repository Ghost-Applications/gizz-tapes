package gizz.tapes.ui.settings

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.api.data.Recording
import gizz.tapes.data.Settings
import gizz.tapes.util.LCE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey(SettingsViewModel::class)
class SettingsViewModel(
    private val dataStore: DataStore<Settings>,
) : ViewModel() {

    val settingsState: StateFlow<LCE<SettingsScreenState, Nothing>> = loadSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(),
        initialValue = LCE.Loading
    )

    private fun loadSettings(): Flow<LCE<SettingsScreenState, Nothing>> = flow {
        dataStore.data
            .map { SettingsScreenState(it.preferredRecordingType) }
            .map { LCE.Content(it) }
            .collect { emit(it) }
    }

    fun updatePreferredRecordingType(type: Recording.Type) {
        viewModelScope.launch {
            dataStore.updateData { it.copy(preferredRecordingType = type) }
        }
    }
}
