package gizz.tapes.data

enum class SortOrder {
    Ascending,
    Descending;

    operator fun not(): SortOrder = when(this) {
        Ascending -> Descending
        Descending -> Ascending
    }
}
