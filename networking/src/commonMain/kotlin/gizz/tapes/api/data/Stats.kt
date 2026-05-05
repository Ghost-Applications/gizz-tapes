package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Stats(
    @SerialName("latest_year")
    val latestYear: Int,
    @SerialName("earliest_year")
    val earliestYear: Int,
    @SerialName("total_shows")
    val totalShows: Int,
    @SerialName("total_recordings")
    val totalRecordings: Int,
    val hours: Int,
    val minutes: Int,
    @SerialName("sbd_count")
    val sbdCount: Int,
    @SerialName("aud_count")
    val audCount: Int
)
