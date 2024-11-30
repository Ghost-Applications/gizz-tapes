package gizz.tapes.ui.menu.settings

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import gizz.tapes.MainDispatcherRule
import gizz.tapes.api.data.Recording.Type
import gizz.tapes.data.Settings
import gizz.tapes.stub
import gizz.tapes.util.LCE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test

class SettingScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `when datastore is loading from disc screen state should be loading`() {
        val classUnderTest = SettingScreenViewModel(
            dataStore = object : DataStore<Settings> by stub() {
                override val data: Flow<Settings> = flow {  }
            }
        )

        val value = classUnderTest.settingsScreenState.value

        assertThat(value).isEqualTo(LCE.Loading)
    }

    @Test
    fun `when datastore data is loaded, the value should be returned in the state`() {
        val classUnderTest = SettingScreenViewModel(
            dataStore = object : DataStore<Settings> {
                val dataStateFlow: MutableStateFlow<Settings> = MutableStateFlow(Settings(Type.SBD))
                override val data: Flow<Settings> = dataStateFlow
                override suspend fun updateData(transform: suspend (t: Settings) -> Settings): Settings {
                    return transform(dataStateFlow.value)
                }
            }
        )

        val value = classUnderTest.settingsScreenState.value
        assertThat(value).isEqualTo(LCE.Content(SettingsScreenState(Type.SBD)))
    }

    @Test
    fun `updatePreferredRecordingType should update saved preferred recording type`() {
        val classUnderTest = SettingScreenViewModel(
            dataStore = object : DataStore<Settings> {
                val dataStateFlow: MutableStateFlow<Settings> = MutableStateFlow(Settings(Type.SBD))
                override val data: Flow<Settings> = dataStateFlow
                override suspend fun updateData(transform: suspend (t: Settings) -> Settings): Settings {
                    val transformed =  transform(dataStateFlow.value)
                    dataStateFlow.emit(transformed)
                    return transformed
                }
            }
        )

        classUnderTest.updatePreferredRecordingType(Type.MTX)

        val value = classUnderTest.settingsScreenState.value
        assertThat(value).isEqualTo(LCE.Content(SettingsScreenState(Type.MTX)))
    }
}
