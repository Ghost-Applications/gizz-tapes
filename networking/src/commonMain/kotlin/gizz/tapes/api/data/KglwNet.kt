package gizz.tapes.api.data

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.path
import kotlinx.serialization.Serializable

@Serializable
data class KglwNet(
    val id: UInt,
    val permalink: String
) {
    companion object {
        const val PERMALINK_PREFIX = "https://kglw.net/setlists/"
    }

    val fullLink get() = "$PERMALINK_PREFIX/$permalink"
}
