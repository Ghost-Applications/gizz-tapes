package gizz.tapes.util

import kotlinx.datetime.LocalDate

fun LocalDate.toSimpleFormat(): String = "$year.$monthNumber.$dayOfMonth"
fun LocalDate.toAlbumFormat(): String = "$year/$monthNumber/$dayOfMonth"
