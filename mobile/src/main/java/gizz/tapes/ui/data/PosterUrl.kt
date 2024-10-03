package gizz.tapes.ui.data

import android.net.Uri

@JvmInline
value class PosterUrl(val value: String) {

    companion object {
        operator fun invoke(value: String?): PosterUrl? = value?.let { PosterUrl(it) }
    }

    override fun toString(): String = value

    fun toUri(): Uri = Uri.parse(value)
}
