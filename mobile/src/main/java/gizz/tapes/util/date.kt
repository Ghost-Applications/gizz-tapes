package gizz.tapes.util

import kotlinx.datetime.LocalDate
import java.text.SimpleDateFormat
import java.util.Locale

private val SIMPLE_DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd", Locale.US)
private val ALBUM_DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd", Locale.US)

fun LocalDate.toSimpleFormat(): String = SIMPLE_DATE_FORMAT.format(this)
fun LocalDate.toAlbumFormat(): String = ALBUM_DATE_FORMAT.format(this)
