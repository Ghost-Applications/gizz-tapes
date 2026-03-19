package gizz.tapes.data

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Year(val value: String) {
    constructor(year: Int) : this(year.toString())

    override fun toString(): String = value

    companion object {
        val navType = object : NavType<Year>(isNullableAllowed = false) {
            override fun get(bundle: SavedState, key: String): Year =
                Year(checkNotNull(bundle.read { getString(key) }))
            override fun put(bundle: SavedState, key: String, value: Year) =
                bundle.write { putString(key, value.value) }
            override fun parseValue(value: String): Year = Year(value)
        }
    }
}
