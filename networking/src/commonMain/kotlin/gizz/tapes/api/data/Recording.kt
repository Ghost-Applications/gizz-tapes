package gizz.tapes.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recording(
    val id: String,
    @SerialName("uploaded_at")
    val uploadedAt: String,
    val type: String?,
    val source: String?,
    val lineage: String?,
    val taper: String?,
    @SerialName("files_path_prefix")
    val filesPathPrefix: String,
    @SerialName("internet_archive")
    val internetArchive: InternetArchive,
    val files: List<Files>
)
