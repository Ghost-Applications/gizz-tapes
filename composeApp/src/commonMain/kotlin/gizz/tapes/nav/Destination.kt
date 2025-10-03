package gizz.tapes.nav

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object YearSelection: Destination
}
