package gizz.tapes.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CastAvailabilityCheckerFoss {
    @Test
    fun shouldNotExist() {
        assertThat(CastAvailabilityChecker.isAvailable).isFalse()
    }
}
