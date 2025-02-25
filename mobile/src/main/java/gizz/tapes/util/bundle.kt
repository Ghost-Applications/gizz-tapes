package gizz.tapes.util

import android.os.Bundle
import gizz.tapes.ui.nav.Show
import kotlinx.serialization.json.Json

fun Show.toExtrasBundle(): Bundle {
    val showInfoString = Json.encodeToString(this)

    return Bundle().apply {
        putString("showInfo", showInfoString)
    }
}

fun Bundle.toShowInfo(): Show {
    return Json.decodeFromString<Show>(checkNotNull(getString("showInfo")))
}
