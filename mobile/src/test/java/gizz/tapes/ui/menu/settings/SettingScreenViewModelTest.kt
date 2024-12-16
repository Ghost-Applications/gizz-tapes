package gizz.tapes.ui.menu.settings

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import gizz.tapes.MainDispatcherRule
import gizz.tapes.api.data.Recording.Type
import gizz.tapes.data.Settings
import gizz.tapes.stub
import gizz.tapes.util.LCE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SettingScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `when datastore is loading from disc screen state should be loading`() = runTest {
        val emittedValues = mutableListOf<LCE<SettingsScreenState, Nothing>>()
        val classUnderTest = SettingScreenViewModel(
            dataStore = object : DataStore<Settings> by stub() {
                override val data: Flow<Settings> = flow {  }
            }
        )

        val job = launch {
            classUnderTest.settingsScreenState.collect { emittedValues.add(it) }
        }
        advanceUntilIdle()
        job.cancelAndJoin()
        assertThat(emittedValues).containsExactly(LCE.Loading)
    }

    @Test
    fun `when datastore data is loaded, the value should be returned in the state`() = runTest {
        val emittedValues = mutableListOf<LCE<SettingsScreenState, Nothing>>()
        val classUnderTest = SettingScreenViewModel(
            dataStore = object : DataStore<Settings> {
                val dataStateFlow: MutableStateFlow<Settings> = MutableStateFlow(Settings())
                override val data: Flow<Settings> = dataStateFlow
                override suspend fun updateData(transform: suspend (t: Settings) -> Settings): Settings {
                    return transform(dataStateFlow.value)
                }
            }
        )

        val job = launch {
            classUnderTest.settingsScreenState.collect { emittedValues.add(it) }
        }
        advanceUntilIdle()
        job.cancelAndJoin()
        assertThat(emittedValues).containsExactly(LCE.Content(SettingsScreenState(Type.SBD)))
    }

    @Test
    fun `updatePreferredRecordingType should update saved preferred recording type`() = runTest {
        val emittedValues = mutableListOf<LCE<SettingsScreenState, Nothing>>()
        val classUnderTest = SettingScreenViewModel(
            dataStore = object : DataStore<Settings> {
                val dataStateFlow: MutableStateFlow<Settings> = MutableStateFlow(Settings())
                override val data: Flow<Settings> = dataStateFlow
                override suspend fun updateData(transform: suspend (t: Settings) -> Settings): Settings {
                    val transformed =  transform(dataStateFlow.value)
                    dataStateFlow.emit(transformed)
                    return transformed
                }
            }
        )

        val job = launch {
            classUnderTest.settingsScreenState.collect { emittedValues.add(it) }
        }
        advanceTimeBy(500.milliseconds)
        classUnderTest.updatePreferredRecordingType(Type.MTX)
        advanceUntilIdle()
        job.cancelAndJoin()
        assertThat(emittedValues).containsExactly(
            LCE.Content(SettingsScreenState(Type.SBD)),
            LCE.Content(SettingsScreenState(Type.MTX))
        )
    }
}
