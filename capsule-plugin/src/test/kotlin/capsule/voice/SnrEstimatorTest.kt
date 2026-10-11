package capsule.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for [SnrEstimator] (CAP-QC-VOICE US-1). Pure — no I/O.
 */
class SnrEstimatorTest {

    @Test
    fun `empty frames estimate to zero`() {
        assertEquals(0.0, SnrEstimator.estimate(doubleArrayOf()))
    }

    @Test
    fun `a loud voice over a quiet floor estimates a high snr`() {
        val snr = SnrEstimator.estimate(doubleArrayOf(0.001, 0.001, 0.5, 0.5, 0.5))
        assertTrue(snr > 50.0, "expected a high SNR, got $snr")
    }

    @Test
    fun `flat frames estimate to about zero`() {
        val snr = SnrEstimator.estimate(doubleArrayOf(0.2, 0.2, 0.2, 0.2))
        assertEquals(0.0, snr, 1e-6)
    }
}
