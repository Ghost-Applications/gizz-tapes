package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowSet(
    val number: Int,
    @SerialName("set_type_id")
    val setTypeId: Int,
    @SerialName("set_type")
    val setType: String,
    val songs: List<SetSong>,
)

@Serializable
data class SetSong(
    val song: Song,
    val position: Int,
    val duration: Float?,
    val footnote: String?,
    @SerialName("is_notable")
    val isNotable: Boolean,
    @SerialName("notable_description")
    val notableDescription: String?,
    val transition: String,
)

@Serializable
data class Song(
    val id: Int,
    val slug: String,
    val name: String,
)
