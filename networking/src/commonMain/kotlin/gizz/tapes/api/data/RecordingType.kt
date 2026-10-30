package gizz.tapes.api.data

import kotlinx.serialization.Serializable

@Serializable
data class RecordingType(
    val id: UInt,
    val name: String,
)
