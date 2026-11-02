package gizz.tapes.nav

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.data.ShowId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// show ids are the date, optionally followed by a suffix when a date has several shows
// (e.g. 2026-08-14stu, 2023-06-08early).
private val showUrlRegex = Regex("""^https://tapes\.kglw\.net/(\d{4}-\d{2}-\d{2}[A-Za-z0-9]*)/?([?#].*)?$""")

fun showIdFromUrl(url: String): ShowId? =
    showUrlRegex.matchEntire(url)?.groupValues?.get(1)?.let(::ShowId)

fun showUrl(id: ShowId): String = "https://tapes.kglw.net/${id.value}"

/**
 * Holds a show opened from a tapes.kglw.net link until the UI navigates to it. A StateFlow so a
 * link received before the UI is composed (cold start) isn't lost.
 */
@Inject
@SingleIn(AppScope::class)
class DeepLinkHandler {
    private val _pendingShow = MutableStateFlow<ShowId?>(null)
    val pendingShow: StateFlow<ShowId?> = _pendingShow.asStateFlow()

    fun handle(url: String): Boolean {
        val showId = showIdFromUrl(url) ?: return false
        _pendingShow.value = showId
        return true
    }

    fun consume() {
        _pendingShow.value = null
    }
}
