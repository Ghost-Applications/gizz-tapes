package gizz.tapes.api.data

import kotlinx.serialization.Serializable

@Serializable
data class KglwNet(
    val id: UInt,
    val permalink: String
)