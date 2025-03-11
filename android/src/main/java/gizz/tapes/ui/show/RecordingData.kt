package gizz.tapes.ui.show

import arrow.core.NonEmptyList

data class RecordingData(
    val notes: String?,
    val selectedRecording: String,
    val recordings: NonEmptyList<RecordingId>,
    val taper: String?,
    val source: String?,
    val lineage: String?,
    val identifier: String,
    val uploadDate: String,
    val kglwNetShowLink: String,
)
