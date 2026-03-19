package gizz.tapes.data

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ShowId(val value: String) {
    override fun toString(): String = value

    companion object {
        val navType = object : NavType<ShowId>(
            isNullableAllowed = false
        ) {
            override fun get(bundle: SavedState, key: String): ShowId =
                ShowId(checkNotNull(bundle.read { getString(key) }))

            override fun put(bundle: SavedState, key: String, value: ShowId) =
                bundle.write { putString(key, value.value) }

            override fun parseValue(value: String): ShowId {
                return ShowId(value)
            }
        }
    }
}
