package gizz.tapes.data

import gizz.tapes.util.toSimpleFormat
import kotlinx.datetime.LocalDate

@JvmInline
value class Subtitle(val value: String) {

    companion object {
        operator fun invoke(date: LocalDate) = Subtitle(date.toSimpleFormat())
    }

    override fun toString(): String = value
}
