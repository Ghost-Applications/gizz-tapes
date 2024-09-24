package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowsData(
    val id: String,
    val date: String,
    @SerialName("venuename")
    val venueName: String,
    val location: String,
    val title: String,
    val order: UShort,
    @SerialName("poster_url")
    val posterUrl: String?
)
