package gizz.tapes.util

import gizz.tapes.api.data.Recording

val List<Recording>.bestRecording: Recording get() = firstOrNull { it.type == Recording.Type.SBD }
    ?: first()
