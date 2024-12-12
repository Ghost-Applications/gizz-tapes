package gizz.tapes.ui.menu.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.util.ResourceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val resourceManager: ResourceManager
): ViewModel() {

    val aboutText: StateFlow<AboutText> = loadAboutText()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AboutText("")
        )

    private fun loadAboutText(): Flow<AboutText> {
        return flow {
            emit(AboutText(resourceManager.loadAboutText()))
        }
    }
}
