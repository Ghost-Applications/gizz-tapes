package gizz.tapes.ui.data

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8

@JvmInline
value class Title(val value: String) {
    val encodedTitle: String get() = value.encodeUtf8().base64Url()

    companion object {
        fun fromEncodedString(encodedTitle: String): Title = Title(
            checkNotNull(encodedTitle.decodeBase64()) {
                "$encodedTitle was not encoded"
            }.utf8()
        )
    }

    override fun toString(): String = value
}

