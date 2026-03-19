package gizz.tapes.data

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Title(val value: String) {
    override fun toString(): String = value
}
