package gizz.tapes

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import gizz.tapes.data.Settings
import okio.FileSystem
import okio.SYSTEM

@SingleIn(AppScope::class)
@DependencyGraph
interface AppGraph : NetworkProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideSettingsDataStore(): DataStore<Settings> {
        OkioStorage<Settings>(
            fileSystem = FileSystem.SYSTEM,
            serializer = OkioSerializer(Settings.serializer()),
        )

        DataStoreFactory.create(
            storage =
        )
    }
}
