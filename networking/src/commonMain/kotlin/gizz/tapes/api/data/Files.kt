package gizz.tapes.api.data

import kotlinx.serialization.Serializable

@Serializable
data class Files(
    val filename: String,
    val length: Float,
    val title: String,
)
