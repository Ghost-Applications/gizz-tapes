package gizz.tapes.storage

import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

val localDateAdapter = object : ColumnAdapter<LocalDate, String> {
    override fun decode(databaseValue: String): LocalDate = LocalDate.parse(databaseValue)
    override fun encode(value: LocalDate): String = value.toString()
}

val durationAdapter = object : ColumnAdapter<Duration, Long> {
    override fun decode(databaseValue: Long): Duration = databaseValue.milliseconds
    override fun encode(value: Duration): Long = value.inWholeMilliseconds
}

val uShortAdapter = object : ColumnAdapter<UShort, Long> {
    override fun decode(databaseValue: Long): UShort = databaseValue.toUShort()
    override fun encode(value: UShort): Long = value.toLong()
}

val instantAdapter = object : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochSeconds(databaseValue)
    override fun encode(value: Instant): Long = value.epochSeconds
}
