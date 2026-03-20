package gizz.tapes.data

import kotlin.jvm.JvmInline

@JvmInline
value class RecordingId(val id: String) {
    override fun toString(): String = id
}
