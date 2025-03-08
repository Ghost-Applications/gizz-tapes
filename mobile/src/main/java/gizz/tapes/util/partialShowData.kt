package gizz.tapes.util

import gizz.tapes.api.data.PartialShowData

val PartialShowData.showTitle: String
    get() = if (title.isNullOrBlank().not()) {
        "$venueName - $title - $location"
    } else {
        "$venueName - $location"
    }
