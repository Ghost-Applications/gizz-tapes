package gizz.tapes.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

fun LocalDate.toSimpleFormat(): String = "$year.${month.number}.$dayOfMonth"
fun LocalDate.toAlbumFormat(): String = "$year/${month.number}/$dayOfMonth"
