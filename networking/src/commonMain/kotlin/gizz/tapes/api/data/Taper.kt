package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Taper(
    val id: UInt,
    val name: String?,
    @SerialName("is_anon")
    val isAnon: Boolean,
    val recordings: List<TaperRecording>,
) {
    @Serializable
    data class TaperRecording(
        val id: String,
        @SerialName("show_id")
        val showId: UInt,
    )
}
