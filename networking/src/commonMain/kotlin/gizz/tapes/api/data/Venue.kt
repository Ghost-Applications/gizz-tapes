package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Venue(
    val id: UInt,
    val slug: String,
    val name: String,
    val city: String,
    val region: String?,
    @SerialName("country_id")
    val countryId: UInt,
    @SerialName("show_count")
    val showCount: UInt
)
