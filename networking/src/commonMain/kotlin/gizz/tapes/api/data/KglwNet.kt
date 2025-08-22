package gizz.tapes.api.data

import kotlinx.serialization.Serializable

@Serializable
data class KglwNet(
    val id: UInt,
    /** Slug to the show, use fullLink to get the full url */
    val permalink: String
) {
    companion object {
        const val PERMALINK_PREFIX = "https://kglw.net/setlists/"
    }

    val fullLink get() = "$PERMALINK_PREFIX/$permalink"
}
