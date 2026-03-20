package gizz.tapes

/**
 * Allow for debug and release builds to implement their own custom logic,
 * like what to log, and what keys to use. Each build variant should override this
 * and provide it with Metro.
 */
fun interface AppInitializer {
    operator fun invoke()
}
