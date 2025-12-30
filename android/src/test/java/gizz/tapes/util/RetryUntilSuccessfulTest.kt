package gizz.tapes.util

import arrow.core.Either
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
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

        assertThat(result).isEqualTo(LCE.Content("Test"))
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

        // Advance time to allow the retries to happen and trigger onErrorAfter3SecondsAction
        // The schedule uses exponential backoff starting at 100ms, so we need to advance
        // enough time for many retries to accumulate 3 seconds of duration
        advanceTimeBy(5.seconds)

        job.join()

        assertThat(onErrorActionCalled).isEqualTo(1)
        assertThat(result).isEqualTo(LCE.Content("Test"))
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

        // Advance time gradually to allow timeouts to occur and retries to accumulate
        // First advance enough for the timeout to trigger
        advanceTimeBy(100.milliseconds)
        // Then advance more time for retries to accumulate 3 seconds of duration
        advanceTimeBy(5.seconds)

        job.join()
        assertThat(job.isCancelled).isTrue()
        assertThat(onErrorActionCalled).isEqualTo(0)
    }

    @Test
    fun `should cancel job successfully when cancelled`() = runTest {
        var onErrorActionCalled = 0

        val job = launch {
            retryUntilSuccessful(
                action = {
                    Either.catchOrThrow {
                        for (i in 1..5) {
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

        assertThat(job.isCancelled).isTrue()
        assertThat(onErrorActionCalled).isEqualTo(0)
    }
}
