package gizz.tapes.util

import android.content.res.Resources
import gizz.tapes.R
import okio.buffer
import okio.source
import javax.inject.Inject
import javax.inject.Singleton

interface ResourceManager {
    suspend fun loadAboutText(): String
}

@Singleton
class RealResourceManager @Inject constructor(
    private val resources: Resources
): ResourceManager {
    override suspend fun loadAboutText(): String {
        return resources.openRawResource(R.raw.about).source()
            .buffer()
            .readUtf8()
    }
}
