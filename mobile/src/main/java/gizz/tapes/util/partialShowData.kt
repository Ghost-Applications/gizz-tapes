package gizz.tapes.util

import gizz.tapes.api.data.PartialShowData

val PartialShowData.showTitle: String
    get() = if (title.isNotBlank()) {
        "$venueName - $title"
    } else {
        venueName
    }
