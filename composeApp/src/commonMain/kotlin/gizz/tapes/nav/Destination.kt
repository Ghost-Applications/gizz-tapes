package gizz.tapes.nav

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Year
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

sealed interface Destination {

    @Serializable
    data object Main : Destination

    @Serializable
    data object Home : Destination

    @Serializable
    data object YearSelection : Destination

    @Serializable
    data class ShowSelection(val selectionType: SelectionType) : Destination {

        @Serializable
        sealed interface SelectionType {
            val title: String

            @Serializable
            data class ByYear(val year: Year) : SelectionType {
                override val title = year.value
            }

            @Serializable
            data class ByVenue(val venueName: String) : SelectionType {
                override val title = venueName
            }
        }

        companion object {
            val selectionTypeNavType = object : NavType<SelectionType>(isNullableAllowed = false) {
                override fun get(bundle: SavedState, key: String): SelectionType {
                    return bundle.read { Json.decodeFromString<SelectionType>(getString(key)) }
                }

                override fun parseValue(value: String): SelectionType {
                    return Json.decodeFromString(value.decodeURLQueryComponent())
                }

                override fun serializeAsValue(value: SelectionType): String {
                    return Json.encodeToString(value).encodeURLPath()
                }

                override fun put(bundle: SavedState, key: String, value: SelectionType) {
                    bundle.write { putString(key, Json.encodeToString(value)) }
                }
            }

            val typeMap = mapOf(typeOf<SelectionType>() to selectionTypeNavType)
        }
    }

    @Serializable
    data class Show(val id: ShowId, val title: FullShowTitle) : Destination {
        companion object {
            val typeMap = mapOf(
                typeOf<ShowId>() to ShowId.navType,
                typeOf<FullShowTitle>() to FullShowTitle.navType
            )
        }
    }

    @Serializable
    data class Player(val showTitle: FullShowTitle) : Destination {
        companion object {
            val typeMap = mapOf(typeOf<FullShowTitle>() to FullShowTitle.navType)
        }
    }

    @Serializable
    data object About : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object Venues : Destination

    @Serializable
    data class CountryVenues(
        val countryId: Int,
        val countryName: String,
    ) : Destination

    @Serializable
    data object Search : Destination
}
