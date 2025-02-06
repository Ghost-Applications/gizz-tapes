@file:Suppress("UNCHECKED_CAST")

package gizz.tapes.util

sealed interface LCE<out CONTENT, out ERROR> {
    data object Loading: LCE<Nothing, Nothing>
    data class Content<out C>(val value: C): LCE<C, Nothing>
    data class Error<E>(val userDisplayedMessage: String, val error: E): LCE<Nothing, E>
}

fun <CONTENT, E> LCE<CONTENT, E>.contentOrNull(): CONTENT? = when (this) {
    is LCE.Content -> value
    else -> null
}

inline fun <CONTENT, E> LCE<CONTENT, E>.onContent(action: (CONTENT) -> Unit): LCE<CONTENT, E> = apply {
    when (this) {
        is LCE.Content -> {
            action(value)
        }

        else -> Unit
    }
}

fun <IN, OUT, E> LCE<IN, E>.map(transform: (IN) -> OUT): LCE<OUT, E> =
    when(this) {
        is LCE.Error -> this
        is LCE.Content -> LCE.Content(transform(value))
        LCE.Loading -> this as LCE<OUT, E>
    }

fun <IN, OUT, E> LCE<List<IN>, E>.mapCollection(transform: (IN) -> OUT): LCE<List<OUT>, E> =
    when(this) {
        is LCE.Error -> this
        is LCE.Content -> LCE.Content(value.map(transform))
        LCE.Loading -> this as LCE<List<OUT>, E>
    }
