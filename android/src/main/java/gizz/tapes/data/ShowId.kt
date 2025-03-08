package gizz.tapes.data

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ShowId(val value: String) {
    override fun toString(): String = value

    companion object {
        val navType = object : NavType<ShowId>(
            isNullableAllowed = false
        ) {
            override fun get(bundle: Bundle, key: String): ShowId {
                return ShowId(checkNotNull(bundle.getString(key)))
            }

            override fun put(bundle: Bundle, key: String, value: ShowId) {
                bundle.putString(key, value.value)
            }

            override fun parseValue(value: String): ShowId {
                return ShowId(value)
            }
        }
    }
}
