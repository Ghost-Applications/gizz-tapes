package gizz.tapes.api.data

import arrow.core.NonEmptyList
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.serialization.NonEmptyListSerializer
import arrow.core.toOption
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.enums.EnumEntries

@Serializable
data class Recording(
    val id: String,
    @SerialName("uploaded_at")
    val uploadedAt: Instant,
    val type: Type,
    val source: String?,
    val lineage: String?,
    val taper: String?,
    @SerialName("files_path_prefix")
    val filesPathPrefix: String,
    @SerialName("internet_archive")
    val internetArchive: InternetArchive,
    @Serializable(NonEmptyListSerializer::class)
    val files: NonEmptyList<Files>
) {
    @Serializable(with = Type.RecordingTypeSerializer::class)
    enum class Type {
        SBD,
        AUD,
        MTX,
        /** Set if there is a value in the json but we don't recognize it in code yet. */
        UnknownType,
        /** null / empty in json */
        None;

        internal object RecordingTypeSerializer: KSerializer<Type> {

            private val delegatingSerializer: KSerializer<String?> = String.serializer().nullable

            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
                serialName = "gizz.tapes.api.data.Recording.Type",
                PrimitiveKind.STRING
            )

            override fun deserialize(decoder: Decoder): Type {
                return decoder.decodeSerializableValue(delegatingSerializer).toOption()
                    .map { stringValue ->
                        entries.find { entry ->
                            entry.name.equals(stringValue, ignoreCase = true)
                        } ?: UnknownType
                    }
                    .getOrElse { None }
            }

            override fun serialize(encoder: Encoder, value: Type) {
               encoder.encodeSerializableValue(delegatingSerializer, value.name)
            }
        }
    }
}


