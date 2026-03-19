package gizz.tapes.data

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import gizz.tapes.util.toAlbumFormat
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FullShowTitle(
    val title: Title,
    val date: LocalDate
) {
    val fullShowTitle = Title("${date.toAlbumFormat()} $title")

    companion object {
        val navType = object : NavType<FullShowTitle>(isNullableAllowed = false) {
            override fun get(bundle: SavedState, key: String): FullShowTitle =
                Json.decodeFromString(checkNotNull(bundle.read { getString(key) }))
            override fun put(bundle: SavedState, key: String, value: FullShowTitle) =
                bundle.write { putString(key, Json.encodeToString(value)) }
            override fun parseValue(value: String): FullShowTitle =
                Json.decodeFromString(value)
            override fun serializeAsValue(value: FullShowTitle): String =
                Json.encodeToString(value)
        }
    }
}
