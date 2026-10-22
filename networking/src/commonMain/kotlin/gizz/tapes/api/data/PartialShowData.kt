package gizz.tapes.api.data

import gizz.tapes.api.serializer.HtmlEntityStringSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartialShowData(
    val id: String,
    val date: LocalDate,
    @SerialName("venuename")
    @Serializable(HtmlEntityStringSerializer::class)
    val venueName: String,
    @Serializable(HtmlEntityStringSerializer::class)
    val location: String,
    @Serializable(HtmlEntityStringSerializer::class)
    val title: String?,
    val order: UShort,
    @SerialName("poster_url")
    val posterUrl: String?,
    @SerialName("average_rating")
    val averageRating: Float?,
    @SerialName("count_ratings")
    val countRatings: Float,
    @SerialName("weighted_rating")
    val weightedRating: Float?,
    val tags: List<UInt>,
    @SerialName("artist_name")
    val artistName: String,
)
