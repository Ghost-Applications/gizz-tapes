package gizz.tapes.ui.menu.settings

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.data.Recording
import gizz.tapes.data.Settings
import gizz.tapes.util.LCE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingScreenViewModel @Inject constructor(
    private val dataStore: DataStore<Settings>,
) : ViewModel() {

    private val _settingScreenState: MutableStateFlow<LCE<SettingsScreenState, Nothing>> =
        MutableStateFlow(LCE.Loading)
    val settingsScreenState: MutableStateFlow<LCE<SettingsScreenState, Nothing>> = _settingScreenState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            dataStore.data.map { it.preferredRecordingType }
                .map { SettingsScreenState(it) }
                .map { LCE.Content(it) }
                .collect {
                    _settingScreenState.emit(it)
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
