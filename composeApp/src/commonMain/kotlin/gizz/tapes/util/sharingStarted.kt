package gizz.tapes.util

import kotlinx.coroutines.flow.SharingStarted
import kotlin.time.Duration.Companion.seconds

val SharingStarted.Companion.ForViewModel get() = SharingStarted.WhileSubscribed(
    5.seconds.inWholeMilliseconds
)
