package capsule

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [Mp4ConversionCommand] (PRT-09 residual — BT.709 colorimetry).
 * Pure — no ffmpeg invoked.
 */
class Mp4ConversionCommandTest {

    @TempDir
    lateinit var dir: File

    private fun input() = File(dir, "deck.webm").apply { writeText("x") }
    private fun output() = File(dir, "deck.mp4")

    @Test
    fun `default argv keeps the historical shape without color flags`() {
        val argv = Mp4ConversionCommand.buildArgv("ffmpeg", input(), output(), bt709 = false)
        assertEquals("ffmpeg", argv.first())
        assertTrue(argv.containsAll(listOf("-c:v", "libx264", "-c:a", "aac", "-movflags", "+faststart")))
        assertFalse(argv.contains("-colorspace"))
        assertFalse(argv.contains("-color_primaries"))
        assertEquals(output().absolutePath, argv.last())
    }

    @Test
    fun `bt709 argv appends the three color flags with bt709`() {
        val argv = Mp4ConversionCommand.buildArgv("ffmpeg", input(), output(), bt709 = true)
        assertEquals("bt709", argv[argv.indexOf("-color_primaries") + 1])
        assertEquals("bt709", argv[argv.indexOf("-color_trc") + 1])
        assertEquals("bt709", argv[argv.indexOf("-colorspace") + 1])
    }

    @Test
    fun `bt709 flags are placed before the output path`() {
        val out = output().absolutePath
        val argv = Mp4ConversionCommand.buildArgv("ffmpeg", input(), output(), bt709 = true)
        assertTrue(argv.indexOf("-colorspace") < argv.indexOf(out))
        assertEquals(out, argv.last())
    }
}
