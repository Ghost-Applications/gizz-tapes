package gizz.tapes.api.data

import gizz.tapes.api.serializer.DurationInSecondsSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class KglwFile(
    val filename: String,
    @Serializable(with = DurationInSecondsSerializer::class)
    val length: Duration,
    val title: String,
)

