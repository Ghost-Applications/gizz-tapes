package gizz.tapes.data

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Year(val value: String) {
    constructor(year: Int): this(year.toString())

    override fun toString(): String = value

    companion object {
        val NavType = object : NavType<Year>(
            isNullableAllowed = false
        ) {
            override fun put(bundle: Bundle, key: String, value: Year) {
                bundle.putString(key, value.value)
            }

            override fun get(bundle: Bundle, key: String): Year {
                return Year(checkNotNull(bundle.getString(key)))
            }

            override fun parseValue(value: String): Year {
                return Year(value)
            }
        }
    }
}
