package gizz.tapes.data

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Year(val value: String) {
    constructor(year: Int): this(year.toString())

    override fun toString(): String = value
}
