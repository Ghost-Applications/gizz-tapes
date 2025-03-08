package gizz.tapes.util

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.json.Json

inline fun <reified T: Any> SavedStateHandle.getDecodedFromString(key: String) : T? {
    return get<String>(key)?.let { it: String ->
        return Json.decodeFromString(it)
    }
}
