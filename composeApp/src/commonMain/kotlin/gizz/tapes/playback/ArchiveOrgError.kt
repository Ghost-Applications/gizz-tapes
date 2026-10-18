package gizz.tapes.playback

fun isArchiveOrgUrl(url: String?): Boolean = url?.contains("archive.org") == true

fun archiveOrgErrorMessage(trackTitle: String?): String {
    val track = trackTitle?.let { "\"$it\"" } ?: "this track"
    return "Couldn't load $track from archive.org. Archive.org may be having issues right now."
}
