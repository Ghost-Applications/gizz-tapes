package gizz.tapes

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class GizzTapesApplication : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var appInitializer: AppInitializer
    @Inject lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        appInitializer()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.i("onTrimMemory: level=%s", level)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
