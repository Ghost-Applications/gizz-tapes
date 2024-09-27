package gizz.tapes

import android.content.Context
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.crashreporter.CrashReporterPlugin
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.leakcanary2.FlipperLeakEventListener
import com.facebook.flipper.plugins.leakcanary2.LeakCanary2FlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import leakcanary.LeakCanary
import timber.log.Timber
import javax.inject.Inject

/** Debug specific initialization things. */
class DebugAppInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkFlipperPlugin: NetworkFlipperPlugin
) : AppInitializer {
    override fun invoke() {
        Timber.plant(Timber.DebugTree())

        SoLoader.init(context, false)

        LeakCanary.config = LeakCanary.config.copy(
            eventListeners = LeakCanary.config.eventListeners + FlipperLeakEventListener(),
        )

        if (FlipperUtils.shouldEnableFlipper(context)) {
            val client = AndroidFlipperClient.getInstance(context)
            client.addPlugin(InspectorFlipperPlugin(context, DescriptorMapping.withDefaults()))
            client.addPlugin(networkFlipperPlugin)
            client.addPlugin(DatabasesFlipperPlugin(context))
            client.addPlugin(SharedPreferencesFlipperPlugin(context))
            client.addPlugin(LeakCanary2FlipperPlugin())
            client.addPlugin(CrashReporterPlugin.getInstance())
            client.start()
        }
    }
}

@InstallIn(SingletonComponent::class)
@Module
abstract class DebugAppInitializerModule {
    @Binds
    abstract fun providesAppInitializer(debugAppInitializer: DebugAppInitializer): AppInitializer
}

