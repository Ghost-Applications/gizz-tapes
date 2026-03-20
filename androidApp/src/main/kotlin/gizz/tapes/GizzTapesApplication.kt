package gizz.tapes

import android.app.Application
import androidx.media3.common.util.UnstableApi
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication

class GizzTapesApplication : Application(), MetroApplication {

    private val appGraph: AndroidAppGraph by lazy {
        createGraphFactory<AndroidAppGraph.Factory>().create(AppContext(this))
    }

    override val appComponentProviders: MetroAppComponentProviders get() = appGraph

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        appGraph.appInitializer()

        Logger.d { PlaybackService::class.java.name }
    }
}
