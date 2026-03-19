package gizz.tapes.util

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RetryUntilSuccessfulTest {
    @Test
    fun `should return content when action succeeds`() = runTest {
        val result = retryUntilSuccessful(
            action = { "Test".right() },
            onErrorAfter3SecondsAction = {
                error("Should not happen")
            }
        )

        assertEquals(LCE.Content("Test"), result)
    }

    @Test
    fun `should return content when action succeeds after multiple errors`() = runTest {
        var onErrorActionCalled = 0
        var errorCount = 0
        var result: LCE.Content<String>? = null

        val job = launch {
            result = retryUntilSuccessful(
                action = {
                    delay(100.milliseconds)
                    Either.catchOrThrow {
                        if (errorCount < 50) {
                            errorCount++
                            @Suppress("TooGenericExceptionThrown")
                            throw RuntimeException("Test")
                        }
                        "Test"
                    }
                },
                onErrorAfter3SecondsAction = {
                    onErrorActionCalled++
                }
            )
        }

        advanceTimeBy(5.seconds)
        job.join()

        assertEquals(1, onErrorActionCalled)
        assertEquals(LCE.Content("Test"), result)
    }

    @Test
    fun `should cancel job on timeout exception`() = runTest {
        var onErrorActionCalled = 0

        val job = launch {
            retryUntilSuccessful(
                action = {
                    Either.catchOrThrow {
                        withTimeout(50.milliseconds) {
                            delay(200.milliseconds)
                        }
                        "Test"
                    }
                },
                onErrorAfter3SecondsAction = {
                    onErrorActionCalled++
                }
            )
        }

        advanceTimeBy(100.milliseconds)
        advanceTimeBy(5.seconds)

        job.join()
        assertTrue(job.isCancelled)
        assertEquals(0, onErrorActionCalled)
    }

    @Test
    fun `should cancel job successfully when cancelled`() = runTest {
        var onErrorActionCalled = 0

        val job = launch {
            retryUntilSuccessful(
                action = {
                    Either.catchOrThrow {
                        repeat(5) {
                            delay(1.seconds)
                        }
                        "Test"
                    }
                },
                onErrorAfter3SecondsAction = {
                    onErrorActionCalled++
                }
            )
        }

        advanceTimeBy(2.seconds)
        job.cancel()

        assertTrue(job.isCancelled)
        assertEquals(0, onErrorActionCalled)
    }
}
