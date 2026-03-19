package gizz.tapes.data

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
value class PosterUrl(val value: String) {

    companion object {
        private val POSTER_MISSING = PosterUrl("https://tapes.kglw.net/assets/img/missing.png")

        operator fun invoke(value: String?): PosterUrl = if (value.isNullOrBlank())
            POSTER_MISSING
        else PosterUrl(value)
    }

    override fun toString(): String = value
}
