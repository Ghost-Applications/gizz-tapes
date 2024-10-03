package gizz.tapes.api.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

object DurationInSecondsSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "gizz.tapes.api.serializer.DurationInSecondsSerializer",
        kind = PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): Duration {
        val value = decoder.decodeDouble()
        return value.seconds
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeDouble(value.toDouble(DurationUnit.SECONDS))
    }
}