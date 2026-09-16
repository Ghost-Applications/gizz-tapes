package gizz.tapes.util

import android.os.Bundle
import gizz.tapes.nav.Destination
import kotlinx.serialization.json.Json

fun Destination.Show.toExtrasBundle(): Bundle {
    return Bundle().apply {
        putString("showInfo", Json.encodeToString(this@toExtrasBundle))
    }
}

fun Bundle.toShowInfo(): Destination.Show {
    return Json.decodeFromString<Destination.Show>(checkNotNull(getString("showInfo")))
}

// Lets the Cast media item converter substitute a fetchable URL when the local player is
// pointed at a downloaded file:// path a Cast receiver can't reach.
fun Bundle.putRemoteUrl(remoteUrl: String): Bundle = apply { putString("remoteUrl", remoteUrl) }
fun Bundle.getRemoteUrl(): String? = getString("remoteUrl")
