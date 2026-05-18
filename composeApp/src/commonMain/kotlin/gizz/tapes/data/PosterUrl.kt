package gizz.tapes.data

import androidx.compose.runtime.Immutable
import gizz_tapes.composeapp.generated.resources.Res
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
value class PosterUrl(val value: String) {

    companion object {
        private val POSTER_MISSING by lazy {
            PosterUrl(
                Res.getUri("drawable/missing.webp")
            )
        }

        operator fun invoke(value: String?): PosterUrl = if (value.isNullOrBlank())
            POSTER_MISSING
        else PosterUrl(value)
    }

    override fun toString(): String = value
}
