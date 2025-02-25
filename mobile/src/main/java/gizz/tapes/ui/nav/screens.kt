package gizz.tapes.ui.nav

import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Year
import gizz.tapes.data.Year.Companion.NavType
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class ShowSelection(
    val year: Year
) {
    companion object {
        val typeMap = mapOf(typeOf<Year>() to NavType)
    }
}

@Serializable
data class Show(
    val id: ShowId,
    val title: FullShowTitle,
) {
    companion object {
        val typeMap = mapOf(
            typeOf<ShowId>() to ShowId.navType,
            typeOf<FullShowTitle>() to FullShowTitle.navType
        )
    }
}

@Serializable
data class Player(
    val showTitle: FullShowTitle
) {
    companion object {
        val typeMap = mapOf(
            typeOf<FullShowTitle>() to FullShowTitle.navType
        )
    }
}

@Serializable data object YearSelection
@Serializable data object About
@Serializable data object Settings
