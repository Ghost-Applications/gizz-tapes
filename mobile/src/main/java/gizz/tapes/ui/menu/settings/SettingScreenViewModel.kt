package gizz.tapes.ui.menu.settings

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.data.Recording
import gizz.tapes.data.Settings
import gizz.tapes.util.LCE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingScreenViewModel @Inject constructor(
    private val dataStore: DataStore<Settings>,
) : ViewModel() {

    val settingsScreenState: StateFlow<LCE<SettingsScreenState, Nothing>> = loadSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = LCE.Loading
        )

    private fun loadSettings(): Flow<LCE<SettingsScreenState, Nothing>> {
        return flow {
            dataStore.data.map { it.preferredRecordingType }
                .map { SettingsScreenState(it) }
                .map { LCE.Content(it) }
                .collect {
                    emit(it)
                }
        }
    }

    fun updatePreferredRecordingType(type: Recording.Type) {
        viewModelScope.launch {
            dataStore.updateData {
                it.copy(preferredRecordingType = type)
            }
        }
    }
}
