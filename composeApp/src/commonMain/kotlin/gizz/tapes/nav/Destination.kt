package gizz.tapes.nav

import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Year
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

sealed interface Destination {

    @Serializable
    data object YearSelection : Destination

    @Serializable
    data class ShowSelection(val year: Year) : Destination {
        companion object {
            val typeMap = mapOf(typeOf<Year>() to Year.navType)
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
}
