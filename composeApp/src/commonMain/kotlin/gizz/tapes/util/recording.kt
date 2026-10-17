package gizz.tapes.util

import arrow.core.NonEmptyList
import gizz.tapes.api.data.Recording

/**
 * Picks the recording whose type ranks highest, breaking ties in favor of favourite tapers.
 * Types missing from the ranking sort last; otherwise the API order is kept.
 */
fun NonEmptyList<Recording>.tryAndGetPreferredRecording(
    rankedTypes: List<Recording.Type>,
    favouriteTapers: Set<String>,
): Recording {
    return sortedWith(
        compareBy<Recording> { rankedTypes.indexOf(it.type).takeIf { index -> index >= 0 } ?: rankedTypes.size }
            .thenBy { it.taper !in favouriteTapers }
    ).first()
}
