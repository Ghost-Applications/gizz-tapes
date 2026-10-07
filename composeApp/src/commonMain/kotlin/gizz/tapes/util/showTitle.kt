package gizz.tapes.util

import gizz.tapes.api.data.PartialShowData
import gizz.tapes.data.BAND_NAME

// the main band is implied, so it's only shown for side projects/solo shows. The API spells it
// "King Gizzard & the Lizard Wizard", hence the case-insensitive match.
val PartialShowData.showTitle: String
    get() = listOfNotNull(
        artistName.takeUnless { it.equals(BAND_NAME, ignoreCase = true) },
        venueName,
        title?.takeIf { it.isNotBlank() },
        location
    ).joinToString(" • ")
