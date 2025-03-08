package gizz.tapes.util

import arrow.core.NonEmptyList
import gizz.tapes.api.data.Recording

fun NonEmptyList<Recording>.tryAndGetPreferredRecordingType(preferred: Recording.Type): Recording {
    return firstOrNull { it.type == preferred } ?: first()
}
