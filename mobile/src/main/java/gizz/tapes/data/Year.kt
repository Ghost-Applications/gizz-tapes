package gizz.tapes.data

@JvmInline
value class Year(val value: String) {
    constructor(year: Int): this(year.toString())

    override fun toString(): String = value
}
