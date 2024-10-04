package gizz.tapes.data

import gizz.tapes.util.toAlbumFormat
import kotlinx.datetime.LocalDate

data class FullShowTitle(
    private val date: LocalDate,
    private val title: Title,
) {
    override fun toString(): String {
        return "${date.toAlbumFormat()} $title"
    }
}
