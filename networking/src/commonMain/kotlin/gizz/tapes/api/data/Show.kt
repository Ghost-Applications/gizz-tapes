package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Show(
    val id: String,
    val order: UShort,
    val date: String,
    @SerialName("poster_url")
    val posterUrl: String?,
    val notes: String?,
    val title: String,
    @SerialName("kglw_net")
    val kglwNet: KglwNet,
    @SerialName("venue_id")
    val venueId: UInt,
    @SerialName("tour_id")
    val tourId: UInt,
    val recordings: List<Recording>
)
