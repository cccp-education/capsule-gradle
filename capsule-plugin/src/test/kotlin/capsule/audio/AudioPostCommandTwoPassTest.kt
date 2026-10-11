package capsule.audio

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for the PRT-09 two-pass loudness commands added to
 * [AudioPostCommand] (pure object — no I/O).
 */
class AudioPostCommandTwoPassTest {

    private val measured = LoudnormMeasurement(
        inputI = -23.03,
        inputTp = -2.19,
        inputLra = 4.30,
        inputThresh = -33.31,
        targetOffset = 0.21,
    )
    private val config = AudioPostConfig(loudnessTarget = -14.0)

    @Test
    fun `two-pass chain injects measured values, gain, limiter and resample`() {
        val chain = AudioPostCommand.buildTwoPassLoudnorm(config, measured)
        assertTrue(chain.contains("loudnorm=I=-14.0:TP=-2:LRA=11"), chain)
        assertTrue(chain.contains("measured_I=-23.03"), chain)
        assertTrue(chain.contains("measured_TP=-2.19"), chain)
        assertTrue(chain.contains("measured_LRA=4.30"), chain)
        assertTrue(chain.contains("measured_thresh=-33.31"), chain)
        assertTrue(chain.contains("offset=0.21"), chain)
        assertTrue(chain.contains("linear=true"), chain)
        assertTrue(chain.contains("volume=1.70dB"), chain)
        assertTrue(chain.contains("alimiter=limit=0.63:level=disabled"), chain)
        assertTrue(chain.contains("aresample=48000"), chain)
    }

    @Test
    fun `two-pass chain honours custom gain and limit`() {
        val chain = AudioPostCommand.buildTwoPassLoudnorm(config, measured, gainDb = 2.5, limit = 0.8)
        assertTrue(chain.contains("volume=2.50dB"), chain)
        assertTrue(chain.contains("alimiter=limit=0.80:level=disabled"), chain)
    }

    @Test
    fun `measure argv decodes to null with loudnorm json`() {
        val args = AudioPostCommand.loudnormMeasureArgs(File("/tmp/x.webm"), config, "ffmpeg")
        assertEquals("ffmpeg", args.first())
        assertTrue(args.any { it.contains("print_format=json") }, args.toString())
        assertTrue(args.containsAll(listOf("-f", "null", "-")), args.toString())
    }

    @Test
    fun `two-pass argv copies video and applies the measured chain`() {
        val args = AudioPostCommand.buildTwoPassArgv(
            File("/tmp/in.webm"), File("/tmp/out.webm"), config, measured, "ffmpeg",
        )
        assertTrue(args.containsAll(listOf("-c:v", "copy")), args.toString())
        assertTrue(args.any { it.contains("measured_I=-23.03") }, args.toString())
        assertEquals("/tmp/out.webm", args.last())
    }
}
