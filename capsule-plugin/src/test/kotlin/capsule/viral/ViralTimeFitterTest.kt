package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral time-fit (CAP-SHORT US-2): [ViralTimeFitter].
 *
 * The storyboard's beats sum to a value within ±20 % of the target (editorial
 * gate). The montage, however, must honour the target **exactly** (15-60 s):
 * the fitter trims or pads the beats proportionally so their sum equals the
 * target, and [ViralTimeFitter.isFitted] gates the produced video's duration.
 *
 * Pure — no Gradle, no I/O, no FFmpeg.
 */
class ViralTimeFitterTest {

    @Test
    fun `fit is the identity when the beats already sum to the target`() {
        val fitted = ViralTimeFitter.fit(listOf(3.0, 10.0, 10.0, 7.0), 30)
        assertEquals(30.0, fitted.sum(), 1e-9)
        assertEquals(listOf(3.0, 10.0, 10.0, 7.0), fitted)
    }

    @Test
    fun `fit pads a short storyboard up to the target (proportional)`() {
        val fitted = ViralTimeFitter.fit(listOf(3.0, 9.0, 9.0, 6.0), 30)
        assertEquals(30.0, fitted.sum(), 1e-9)
        // 27 -> 30, scale 30/27 = 1.111...
        assertEquals(3.0 * 30 / 27, fitted[0], 1e-9)
        assertEquals(9.0 * 30 / 27, fitted[1], 1e-9)
    }

    @Test
    fun `fit trims a long storyboard down to the target (proportional)`() {
        val fitted = ViralTimeFitter.fit(listOf(3.0, 10.0, 10.0, 7.0), 27)
        assertEquals(27.0, fitted.sum(), 1e-9)
        // 30 -> 27, scale 0.9
        assertEquals(2.7, fitted[0], 1e-9)
        assertEquals(9.0, fitted[2], 1e-9)
    }

    @Test
    fun `fit clamps the target to the short-form range`() {
        assertEquals(60.0, ViralTimeFitter.fit(listOf(30.0), 300).sum(), 1e-9)
        assertEquals(15.0, ViralTimeFitter.fit(listOf(30.0), 1).sum(), 1e-9)
    }

    @Test
    fun `fit rejects an empty or non-positive storyboard`() {
        assertTrue(runCatching { ViralTimeFitter.fit(emptyList(), 30) }.isFailure)
        assertTrue(runCatching { ViralTimeFitter.fit(listOf(3.0, 0.0), 30) }.isFailure)
    }

    @Test
    fun `fit is deterministic`() {
        val source = listOf(3.0, 9.0, 9.0, 6.0)
        assertEquals(ViralTimeFitter.fit(source, 30), ViralTimeFitter.fit(source, 30))
    }

    @Test
    fun `isFitted accepts within tolerance and rejects beyond`() {
        assertTrue(ViralTimeFitter.isFitted(30.4, 30, 1.0))
        assertTrue(ViralTimeFitter.isFitted(30.0, 30, 0.0))
        assertFalse(ViralTimeFitter.isFitted(32.0, 30, 1.0))
    }
}
