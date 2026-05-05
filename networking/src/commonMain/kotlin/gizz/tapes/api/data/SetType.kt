package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetType(
    val id: Int,
    val name: String,
    @SerialName("show_count")
    val showCount: Int,
)
