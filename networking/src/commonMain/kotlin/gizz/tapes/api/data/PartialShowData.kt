package gizz.tapes.api.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartialShowData(
    val id: String,
    val date: LocalDate,
    @SerialName("venuename")
    val venueName: String,
    val location: String,
    val title: String?,
    val order: UShort,
    @SerialName("poster_url")
    val posterUrl: String?
)
