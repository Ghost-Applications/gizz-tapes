package gizz.tapes.ui.components

import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId

fun interface ShowClick {
    operator fun invoke(showId: ShowId, showTitle: FullShowTitle)
}
