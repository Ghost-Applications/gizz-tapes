package gizz.tapes.api.data

import gizz.tapes.api.serializer.HtmlEntityStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Venue(
    val id: UInt,
    val slug: String,
    @Serializable(HtmlEntityStringSerializer::class)
    val name: String,
    @Serializable(HtmlEntityStringSerializer::class)
    val city: String,
    @Serializable(HtmlEntityStringSerializer::class)
    val region: String?,
    @SerialName("country_id")
    val countryId: UInt,
    @SerialName("show_count")
    val showCount: UInt
)
