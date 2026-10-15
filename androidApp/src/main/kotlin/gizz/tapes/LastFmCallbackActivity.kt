package gizz.tapes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import gizz.tapes.lastfm.LastFmAuthManager

/**
 * Receives the `gizztapes://lastfm` redirect after the user approves Gizz Tapes on last.fm, then
 * brings the existing app task back instead of stacking a new MainActivity on the browser.
 */
@Inject
@ActivityKey
@ContributesIntoMap(AppScope::class, binding<Activity>())
class LastFmCallbackActivity(
    private val lastFmAuthManager: LastFmAuthManager,
) : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastFmAuthManager.onCallback()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
        )
        finish()
    }
}
