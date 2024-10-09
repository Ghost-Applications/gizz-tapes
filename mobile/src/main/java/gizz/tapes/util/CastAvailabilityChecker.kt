package gizz.tapes.util

object CastAvailabilityChecker {
    val isAvailable: Boolean by lazy {
        runCatching {
            Class.forName("com.google.android.gms.cast.framework.CastButtonFactory")
        }.map { true }
            .getOrDefault(false)
    }
}
