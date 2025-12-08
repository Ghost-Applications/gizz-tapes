package gizz.tapes

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import gizz.tapes.data.Settings
import gizz.tapes.data.SettingsSerializer
import okio.FileSystem
import okio.SYSTEM

@SingleIn(AppScope::class)
@DependencyGraph
interface AppGraph : NetworkProviders, ViewModelGraph {

    @Provides
    @SingleIn(AppScope::class)
    fun provideSettingsDataStore(): DataStore<Settings> {
        val okioStorage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = SettingsSerializer(),
            producePath = { TODO() }
        )

       return DataStoreFactory.create(
            storage = okioStorage,
        )
    }
}
