package gizz.tapes.data

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Title(val value: String) {
    companion object {
        val navType = object : NavType<Title>(
            isNullableAllowed = false
        ) {
            override fun get(bundle: Bundle, key: String): Title {
                return Title(checkNotNull(bundle.getString(key)))
            }

            override fun put(bundle: Bundle, key: String, value: Title) {
                bundle.putString(key, value.value)
            }

            override fun serializeAsValue(value: Title): String = Uri.encode(value.value)

            override fun parseValue(value: String): Title = Title(value)
        }
    }

    override fun toString(): String = value
}
