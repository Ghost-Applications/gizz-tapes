package gizz.tapes.util

import android.os.Bundle
import gizz.tapes.ui.data.ShowId
import gizz.tapes.ui.data.Title

fun Bundle.toShowInfo(): Pair<ShowId, Title> =
    ShowId(getString("showId", "missing show id")) to
            Title(getString("showTitle", "missing show title"))
