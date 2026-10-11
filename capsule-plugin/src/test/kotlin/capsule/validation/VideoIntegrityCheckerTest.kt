package capsule.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * TDD unit tests for [VideoIntegrityChecker] (PRT-04).
 *
 * The checker is a pure domain object: given the raw outcome of a decoding
 * command (exit code + diagnostic), it decides whether the media is decodable.
 * No I/O, no ffmpeg — the numbers are passed directly.
 */
class VideoIntegrityCheckerTest {

    @Test
    fun `clean when decoder exits zero with no diagnostic`() {
        val result = VideoIntegrityChecker.verdict(exitCode = 0, stderr = "")
        assertIs<IntegrityResult.Clean>(result)
        assertTrue(result.isClean)
    }

    @Test
    fun `clean when diagnostic is only whitespace`() {
        val result = VideoIntegrityChecker.verdict(exitCode = 0, stderr = "  \n\t ")
        assertIs<IntegrityResult.Clean>(result)
    }

    @Test
    fun `corrupt when decoder exits non-zero with no diagnostic`() {
        val result = VideoIntegrityChecker.verdict(exitCode = 1, stderr = "")
        assertIs<IntegrityResult.Corrupt>(result)
        assertFalse(result.isClean)
        assertEquals("", result.stderrTail)
    }

    @Test
    fun `corrupt when decoder exits zero but printed a diagnostic`() {
        val result = VideoIntegrityChecker.verdict(exitCode = 0, stderr = "moov atom not found")
        assertIs<IntegrityResult.Corrupt>(result)
        assertEquals("moov atom not found", result.stderrTail)
    }

    @Test
    fun `corrupt diagnostic is trimmed`() {
        val result = VideoIntegrityChecker.verdict(exitCode = 1, stderr = "\n  invalid data  \n")
        assertIs<IntegrityResult.Corrupt>(result)
        assertEquals("invalid data", result.stderrTail)
    }

    @Test
    fun `corrupt when decoder times out`() {
        val result = VideoIntegrityChecker.verdict(
            exitCode = capsule.ProcessRunner.TIMED_OUT,
            stderr = "killed"
        )
        assertIs<IntegrityResult.Corrupt>(result)
    }
}
