package gizz.tapes.data

import arrow.core.NonEmptyList
import gizz.tapes.api.data.Recording

data class RecordingData(
    val notes: String?,
    val recordings: NonEmptyList<Option>,
    val taper: String?,
    val source: String?,
    val lineage: String?,
    val id: RecordingId,
    val uploadDate: String,
    val kglwNetShowLink: String,
    val type: Recording.Type
) {
    val label: String get() = recordingLabel(id, type, taper)

    data class Option(
        val id: RecordingId,
        val type: Recording.Type,
        val taper: String?,
    ) {
        val label: String get() = recordingLabel(id, type, taper)
    }
}

private fun recordingLabel(id: RecordingId, type: Recording.Type, taper: String?): String =
    "${type.name}${taper?.let { " by $it" }.orEmpty()}: ${id.id}"
