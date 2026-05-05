package gizz.tapes.api.data

import kotlinx.serialization.Serializable

@Serializable
data class HeroPhoto(
    val credit: String,
    val url: String,
    val vPosition: UShort,
)
