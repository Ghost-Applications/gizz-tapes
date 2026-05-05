package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YearData(
    val year: Int,
    @SerialName("show_count")
    val showCount: Int,
    @SerialName("poster_url")
    val posterUrl: String?
)
