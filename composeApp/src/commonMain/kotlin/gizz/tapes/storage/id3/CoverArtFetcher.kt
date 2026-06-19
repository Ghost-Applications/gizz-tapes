package gizz.tapes.storage.id3

import co.touchlab.kermit.Logger
import gizz_tapes.composeapp.generated.resources.Res
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.contentType

private val logger = Logger.withTag("CoverArtFetcher")

private const val PLACEHOLDER_MIME_TYPE = "image/webp"

// posterUrl can be a bundled placeholder resource URI (missing.webp) rather than a real network
// URL when a show has no poster. Falls back to that same bundled placeholder image (rather than
// no cover art at all) both when there's no real poster and when fetching one fails.
suspend fun fetchCoverArt(httpClient: HttpClient, posterUrl: String?): Id3CoverArt? {
    if (posterUrl != null && posterUrl.startsWith("http")) {
        val fetched = runCatching {
            val response = httpClient.get(posterUrl)
            Id3CoverArt(bytes = response.body(), mimeType = response.contentType()?.toString() ?: "image/jpeg")
        }.onFailure { logger.e(it) { "Failed to fetch cover art from $posterUrl" } }.getOrNull()
        if (fetched != null) return fetched
    }

    return runCatching { Id3CoverArt(bytes = Res.readBytes("drawable/missing.webp"), mimeType = PLACEHOLDER_MIME_TYPE) }
        .onFailure { logger.e(it) { "Failed to load the bundled placeholder cover art" } }
        .getOrNull()
}
