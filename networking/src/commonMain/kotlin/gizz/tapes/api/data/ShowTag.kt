package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowTag(
    val id: UInt,
    val slug: String,
    val name: String,
    @SerialName("show_count")
    val showCount: UInt,
)
