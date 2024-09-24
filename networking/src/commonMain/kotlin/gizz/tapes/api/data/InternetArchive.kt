package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InternetArchive(
    @SerialName("is_lma")
    val isLma: Boolean
)
