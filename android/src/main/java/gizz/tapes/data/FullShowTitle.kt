package gizz.tapes.data

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
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
        val navType = object : NavType<FullShowTitle>(
            isNullableAllowed = false
        ) {
            override fun get(bundle: Bundle, key: String): FullShowTitle {
                return Json.decodeFromString<FullShowTitle>(checkNotNull(bundle.getString(key)))
            }

            override fun put(bundle: Bundle, key: String, value: FullShowTitle) {
                bundle.putString(key, Json.encodeToString(value))
            }

            override fun serializeAsValue(value: FullShowTitle): String {
                return Uri.encode(Json.encodeToString(value))
            }

            override fun parseValue(value: String): FullShowTitle = Json.decodeFromString(value)
        }
    }
}
