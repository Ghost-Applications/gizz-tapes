package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val id: UInt,
    val name: String,
    @SerialName("show_count")
    val showCount: UInt,
)
