package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.io.File

/**
 * TDD unit tests for [VideoFormatConverter] and [CapsuleManager.resolveFormatConverter]
 * (CAP-MP4 US-2).
 *
 * [VideoFormatConverter] is the FFmpeg-backed WebM→MP4/H.264 transcode engine.
 * [NoOpVideoFormatConverter] is the test fallback. The factory
 * [CapsuleManager.resolveFormatConverter] mirrors [CapsuleManager.resolveSubtitleBurnInService]
 * (ffmpegPath="noop" → NoOp, unavailable + non-strict → NoOp, unavailable + strict → throw).
 */
class VideoFormatConverterTest {

    @Test
    fun `NoOpVideoFormatConverter returns false from convertToMp4`() {
        val converter = NoOpVideoFormatConverter()
        val webm = File.createTempFile("test", ".webm").apply { writeText("dummy") }
        val mp4 = File.createTempFile("out", ".mp4")
        try {
            val result = converter.convertToMp4(webm, mp4)
            assertFalse(result, "NoOp converter should return false (no real transcode)")
            assertFalse(mp4.exists() && mp4.length() > 0, "NoOp should not produce a real mp4")
        } finally {
            webm.delete(); mp4.delete()
        }
    }

    @Test
    fun `NoOpVideoFormatConverter isAvailable returns true and names noop-format`() {
        val converter = NoOpVideoFormatConverter()
        assertTrue(converter.isAvailable(), "NoOp is always available (fallback)")
        assertEquals("noop-format", converter.name())
    }

    @Test
    fun `VideoFormatConverterImpl with noop path returns unavailable`() {
        val converter = VideoFormatConverterImpl(ffmpegPath = "/nonexistent/ffmpeg/path_xyz")
        assertFalse(converter.isAvailable(), "ffmpeg at a bogus path should not be available")
        assertEquals("ffmpeg-format", converter.name())
    }

    @Test
    fun `resolveFormatConverter with ffmpegPath noop returns NoOp`() {
        val resolved = CapsuleManager.resolveFormatConverter(ffmpegPath = "noop", strict = false)
        assertTrue(resolved is NoOpVideoFormatConverter, "noop path should return NoOp")
        assertEquals("noop-format", resolved.name())
    }

    @Test
    fun `resolveFormatConverter unavailable and non-strict falls back to NoOp`() {
        val resolved = CapsuleManager.resolveFormatConverter(
            ffmpegPath = "/nonexistent/ffmpeg/path_xyz",
            strict = false
        )
        assertTrue(resolved is NoOpVideoFormatConverter, "unavailable + non-strict → NoOp fallback")
    }

    @Test
    fun `resolveFormatConverter unavailable and strict throws IllegalStateException`() {
        val ex = assertFailsWith<IllegalStateException> {
            CapsuleManager.resolveFormatConverter(
                ffmpegPath = "/nonexistent/ffmpeg/path_xyz",
                strict = true
            )
        }
        assertTrue(ex.message!!.contains("ffmpeg"), "error should name ffmpeg")
        assertTrue(ex.message!!.contains("/nonexistent/ffmpeg/path_xyz"), "error should mention the path")
        assertTrue(ex.message!!.contains("strictMode"), "error should suggest disabling strictMode")
    }

    @Test
    fun `resolveFormatConverter named ffmpeg-format when impl returned`() {
        // We cannot guarantee ffmpeg is on the test PATH, so only assert the name
        // contract when the impl is actually available.
        val impl = VideoFormatConverterImpl("ffmpeg")
        if (impl.isAvailable()) {
            val resolved = CapsuleManager.resolveFormatConverter("ffmpeg", strict = false)
            assertEquals("ffmpeg-format", resolved.name())
        }
    }
}