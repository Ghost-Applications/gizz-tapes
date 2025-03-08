package gizz.tapes.util

import arrow.core.Either
import arrow.core.getOrElse
import arrow.resilience.Schedule
import arrow.resilience.retryEither
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Retries the provided action until a successful result can be returned. After 3 seconds of
 * errors the onErrorAfter3SecondsAction will be performed in order to do some kind of other
 * action while waiting.
 */
suspend inline fun <Output> retryUntilSuccessful(
    action: () -> Either<Throwable, Output>,
    crossinline onErrorAfter3SecondsAction: suspend (error: Throwable) -> Unit,
): LCE.Content<Output> {
    return Schedule.exponential<Throwable>(100.milliseconds).doWhile { _, duration -> duration < 3.seconds }
        .andThen(
            Schedule.doWhile { error, _ ->
                onErrorAfter3SecondsAction(error)
                false
            }
        )
        .andThen(Schedule.spaced(3.seconds))
        .retryEither(action)
        .map { LCE.Content(it) }
        .getOrElse { error("This shouldn't happen retry should continue forever") }
}
