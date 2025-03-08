package gizz.tapes.api.data

import arrow.core.NonEmptyList
import arrow.core.serialization.NonEmptyListSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Show(
    val id: String,
    val order: UShort,
    val date: LocalDate,
    @SerialName("poster_url")
    val posterUrl: String?,
    val notes: String?,
    val title: String?,
    @SerialName("kglw_net")
    val kglwNet: KglwNet,
    @SerialName("venue_id")
    val venueId: UInt,
    @SerialName("tour_id")
    val tourId: UInt,
    @Serializable(NonEmptyListSerializer::class)
    val recordings: NonEmptyList<Recording>
)
