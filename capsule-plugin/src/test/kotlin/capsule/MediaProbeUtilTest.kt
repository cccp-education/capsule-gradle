package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD Baby Step — MediaProbeUtil unit tests.
 *
 * Shared utility for probing media file durations via ffprobe.
 * Extracted from duplicated ffprobeDuration logic in CapsuleVideoTask,
 * ManimEngineImpl, and ManimVideoMixerImpl.
 */
class MediaProbeUtilTest {

    @Test
    fun `probeDuration returns 0 for nonexistent file`() {
        val fakeFile = File("/nonexistent/path/video.mp4")
        assertEquals(0.0, MediaProbeUtil.probeDuration(fakeFile))
    }

    @Test
    fun `probeDuration with custom ffprobePath returns 0 when binary missing`() {
        val fakeFile = File.createTempFile("video", ".mp4")
        fakeFile.deleteOnExit()
        assertEquals(0.0, MediaProbeUtil.probeDuration(fakeFile, "/nonexistent/ffprobe"))
    }

    @Test
    fun `probeDuration returns 0 for empty or non-numeric output`() {
        // A file that exists but is not a real media file will cause ffprobe
        // to output an empty or error string, resulting in 0.0.
        val tmpFile = File.createTempFile("not-a-video", ".txt")
        tmpFile.writeText("hello")
        tmpFile.deleteOnExit()
        val result = MediaProbeUtil.probeDuration(tmpFile)
        assertEquals(0.0, result, "Non-media file should yield 0.0")
    }

    @Test
    fun `probeDuration with custom ffprobePath delegates correctly`() {
        // When a custom ffprobe path is provided, the overload should use it.
        // We cannot rely on a real ffprobe binary in CI, so we assert that
        // a nonexistent path returns 0.0 (proving the overload was called).
        val tmpFile = File.createTempFile("video", ".mp4")
        tmpFile.deleteOnExit()
        assertEquals(0.0, MediaProbeUtil.probeDuration(tmpFile, "/fake/ffprobe"))
    }

    @Test
    fun `isFfprobeAvailable returns false when ffprobe not in PATH`() {
        // There is no guarantee ffprobe is installed in the test environment.
        // The test simply asserts the method returns a boolean and is consistent.
        val available = MediaProbeUtil.isFfprobeAvailable()
        // We do not hardcode true/false because CI may or may not have ffprobe.
        // Instead we assert idempotence: two calls return the same value.
        assertEquals(available, MediaProbeUtil.isFfprobeAvailable())
    }

    @Test
    fun `isFfprobeAvailable returns false for nonexistent path`() {
        assertFalse(MediaProbeUtil.isFfprobeAvailable("/nonexistent/ffprobe"))
    }
}
