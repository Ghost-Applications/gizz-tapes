package gizz.tapes.data

import arrow.core.NonEmptyList
import gizz.tapes.api.data.Recording

data class RecordingData(
    val notes: String?,
    val recordings: NonEmptyList<RecordingId>,
    val taper: String?,
    val source: String?,
    val lineage: String?,
    val id: RecordingId,
    val uploadDate: String,
    val kglwNetShowLink: String,
    val type: Recording.Type
)
