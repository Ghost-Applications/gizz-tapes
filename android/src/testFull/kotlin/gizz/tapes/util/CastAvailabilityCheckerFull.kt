package gizz.tapes.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CastAvailabilityCheckerFull {
    @Test
    fun shouldNotExist() {
        assertThat(CastAvailabilityChecker.isAvailable).isTrue()
    }
}
