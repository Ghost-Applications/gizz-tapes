package gizz.tapes.api.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Some API strings come through HTML-escaped (e.g. "&#039;s-Hertogenbosch"), this decodes them.
 */
object HtmlEntityStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "gizz.tapes.api.serializer.HtmlEntityStringSerializer",
        kind = PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): String = decoder.decodeString().decodeHtmlEntities()

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

private val namedEntities = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to " ",
)

private val entityRegex = Regex("&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")

internal fun String.decodeHtmlEntities(): String {
    if ('&' !in this) return this
    return entityRegex.replace(this) { match ->
        val entity = match.groupValues[1]
        val codePoint = when {
            entity.startsWith("#x", ignoreCase = true) -> entity.drop(2).toIntOrNull(16)
            entity.startsWith("#") -> entity.drop(1).toIntOrNull()
            else -> null
        }
        when {
            codePoint != null && codePoint in 0..0x10FFFF -> codePoint.toCodePointString()
            else -> namedEntities[entity] ?: match.value
        }
    }
}

private fun Int.toCodePointString(): String = if (this < 0x10000) {
    this.toChar().toString()
} else {
    val offset = this - 0x10000
    charArrayOf(
        (0xD800 + (offset shr 10)).toChar(),
        (0xDC00 + (offset and 0x3FF)).toChar(),
    ).concatToString()
}
