package gizz.tapes.data

@JvmInline
value class ShowId(val value: String) {
    override fun toString(): String = value
}
