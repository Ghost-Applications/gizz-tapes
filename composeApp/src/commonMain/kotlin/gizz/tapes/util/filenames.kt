package gizz.tapes.util

// Also guards against a bare "." or ".." - stripping /\:*?"<>| alone doesn't touch either, but
// used as a path segment (e.g. ShowSaver.localPath()) they'd walk out of the intended directory.
fun sanitizeFileName(name: String): String {
    val cleaned = name.replace(Regex("""[/\\:*?"<>|]"""), "_").trim()
    return if (cleaned.isEmpty() || cleaned == "." || cleaned == "..") "_" else cleaned
}
