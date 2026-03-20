package com.google.android.gms.cast.framework

@Suppress("UnusedParameter")
class CastContext {
    companion object {
        fun getSharedInstance(playbackService: Any, directExecutor: Any): CastContext {
            error("Not implemented in FOSS variant")
        }
    }

    fun addOnFailureListener(input: (Exception) -> Unit): CastContext {
        error("Not implemented in FOSS variant")
    }

    val result: Any = error("Not implemented in FOSS variant")
}
