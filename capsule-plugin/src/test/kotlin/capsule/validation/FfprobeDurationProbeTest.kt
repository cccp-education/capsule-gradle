package capsule.validation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * TDD — VK-4 US-4a: capsule's [FfprobeDurationProbe] implements the shared N1
 * [video.kernel.VideoDurationProbe] port, reusing the kernel parser.
 *
 * The external process is injected (no real ffprobe needed).
 */
class FfprobeDurationProbeTest {

    @Test
    fun `parses the probed duration through the kernel parser`() {
        val probe = FfprobeDurationProbe("ffprobe", runner = { "12.5\n" })
        val video = File.createTempFile("video", ".mp4").apply { writeText("x"); deleteOnExit() }
        assertEquals(12.5, probe.probeSeconds(video))
    }

    @Test
    fun `unparsable probe output degrades to zero`() {
        val probe = FfprobeDurationProbe("ffprobe", runner = { "N/A\n" })
        val video = File.createTempFile("video", ".mp4").apply { writeText("x"); deleteOnExit() }
        assertEquals(0.0, probe.probeSeconds(video))
    }

    @Test
    fun `a failing runner never throws`() {
        val probe = FfprobeDurationProbe("ffprobe", runner = { throw RuntimeException("boom") })
        val video = File.createTempFile("video", ".mp4").apply { writeText("x"); deleteOnExit() }
        assertEquals(0.0, probe.probeSeconds(video))
    }

    @Test
    fun `a missing file is zero without invoking the runner`() {
        var invoked = false
        val probe = FfprobeDurationProbe("ffprobe", runner = { invoked = true; "9.0" })
        assertEquals(0.0, probe.probeSeconds(File("/nonexistent/video.mp4")))
        assertFalse(invoked, "the runner must not run for a missing file")
    }

    @Test
    fun `an empty file is zero without invoking the runner`() {
        var invoked = false
        val probe = FfprobeDurationProbe("ffprobe", runner = { invoked = true; "9.0" })
        val empty = File.createTempFile("empty", ".mp4").apply { deleteOnExit() }
        assertEquals(0.0, probe.probeSeconds(empty))
        assertFalse(invoked, "the runner must not run for an empty file")
    }
}
