package gizz.tapes.data

import androidx.compose.runtime.Immutable

@Immutable
@JvmInline
value class Year(val value: String) {
    constructor(year: Int): this(year.toString())

    override fun toString(): String = value
}
