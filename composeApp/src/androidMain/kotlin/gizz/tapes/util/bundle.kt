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
