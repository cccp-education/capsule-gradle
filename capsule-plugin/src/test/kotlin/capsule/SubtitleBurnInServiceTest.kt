package capsule

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * TDD Baby Step — SubtitleBurnInService unit tests.
 *
 * SubtitleBurnInService burns SRT/VTT subtitle files into WebM videos
 * using FFmpeg's subtitles filter.
 *
 * Design:
 * - Interface SubtitleBurnInService with burnIn(video, subtitle, output)
 * - NoOpSubtitleBurnInService creates placeholder for testing
 * - SubtitleBurnInServiceImpl uses ffmpeg subtitles filter
 * - Factory: CapsuleManager.resolveSubtitleBurnInService(ffmpegPath)
 */
class SubtitleBurnInServiceTest {

    // ─── NoOpSubtitleBurnInService ────────────────────────────────

    @Test
    fun `noop burnin is available and named noop-burnin`() {
        val service = NoOpSubtitleBurnInService()
        assertTrue(service.isAvailable())
        assertEquals("noop-burnin", service.name())
    }

    @Test
    fun `noop burnin creates placeholder file with metadata`() {
        val service = NoOpSubtitleBurnInService()
        val tmpDir = File.createTempFile("burnin-test", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val videoFile = File(tmpDir, "deck.webm")
        videoFile.writeText("fake webm video")
        val subtitleFile = File(tmpDir, "deck.srt")
        subtitleFile.writeText("1\n00:00:00,000 --> 00:00:05,000\nHello")

        val outputFile = File(tmpDir, "burned.webm")
        val result = service.burnIn(videoFile, subtitleFile, outputFile)

        assertTrue(result.exists(), "NoOp burnin should produce output file")
        assertEquals("burned.webm", result.name)
        val content = result.readText()
        assertTrue(content.contains("SUBTITLE BURN-IN PLACEHOLDER"), "Should contain placeholder marker")
        assertTrue(content.contains("deck.webm"), "Should mention source video")
        assertTrue(content.contains("deck.srt"), "Should mention source subtitle")
    }

    @Test
    fun `noop burnin creates parent directories for output`() {
        val service = NoOpSubtitleBurnInService()
        val tmpDir = File.createTempFile("burnin-mkdir", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val videoFile = File(tmpDir, "input.webm")
        videoFile.writeText("fake")
        val subtitleFile = File(tmpDir, "input.srt")
        subtitleFile.writeText("fake")

        val nestedOutput = File(tmpDir, "nested/sub/dir/output.webm")
        val result = service.burnIn(videoFile, subtitleFile, nestedOutput)

        assertTrue(result.exists(), "Should create nested output directories")
        assertTrue(result.parentFile.exists(), "Parent directory should exist")
    }

    // ─── SubtitleBurnInServiceImpl (without real ffmpeg) ──────────

    @Test
    fun `burnin impl reports unavailable when ffmpeg not found`() {
        val service = SubtitleBurnInServiceImpl(ffmpegPath = "/nonexistent/path/ffmpeg")
        assertFalse(service.isAvailable(), "Should report unavailable for nonexistent ffmpeg")
    }

    @Test
    fun `burnin impl name returns ffmpeg-burnin`() {
        val service = SubtitleBurnInServiceImpl()
        assertEquals("ffmpeg-burnin", service.name())
    }

    @Test
    fun `burnin impl throws BurnInException when ffmpeg not available and burnIn called`() {
        val service = SubtitleBurnInServiceImpl(ffmpegPath = "/nonexistent/path/ffmpeg")
        val tmpDir = File.createTempFile("burnin-exc", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val videoFile = File(tmpDir, "deck.webm")
        videoFile.writeText("fake")
        val subtitleFile = File(tmpDir, "deck.srt")
        subtitleFile.writeText("1\n00:00:00,000 --> 00:00:05,000\nHello")
        val outputFile = File(tmpDir, "output.webm")

        var exceptionCaught = false
        try {
            service.burnIn(videoFile, subtitleFile, outputFile)
        } catch (e: BurnInException) {
            exceptionCaught = true
            assertTrue(e.message!!.contains("ffmpeg"), "Exception should mention ffmpeg")
        }
        assertTrue(exceptionCaught, "Should throw BurnInException when ffmpeg unavailable")
    }

    @Test
    fun `burnin impl throws BurnInException when subtitle file does not exist`() {
        val service = SubtitleBurnInServiceImpl(ffmpegPath = "/nonexistent/path/ffmpeg")
        val tmpDir = File.createTempFile("burnin-nosub", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val videoFile = File(tmpDir, "deck.webm")
        videoFile.writeText("fake")
        val missingSubtitle = File(tmpDir, "nonexistent.srt")
        val outputFile = File(tmpDir, "output.webm")

        var exceptionCaught = false
        try {
            service.burnIn(videoFile, missingSubtitle, outputFile)
        } catch (e: BurnInException) {
            exceptionCaught = true
            assertTrue(e.message!!.contains("not found"), "Exception should mention subtitle not found")
        }
        assertTrue(exceptionCaught, "Should throw BurnInException when subtitle missing")
    }

    // ─── resolveSubtitleBurnInService factory ─────────────────────

    @Test
    fun `resolveSubtitleBurnInService returns NoOp when ffmpeg is noop`() {
        val service = CapsuleManager.resolveSubtitleBurnInService("noop")
        assertTrue(service is NoOpSubtitleBurnInService, "Should return NoOp when ffmpegPath is 'noop'")
    }

    @Test
    fun `resolveSubtitleBurnInService returns NoOp when ffmpeg not found`() {
        val service = CapsuleManager.resolveSubtitleBurnInService("/nonexistent/path/ffmpeg")
        assertTrue(service is NoOpSubtitleBurnInService, "Should return NoOp when ffmpeg not found")
    }

    @Test
    fun `resolveSubtitleBurnInService does not throw with default ffmpeg`() {
        val service = CapsuleManager.resolveSubtitleBurnInService("ffmpeg")
        assertNotNull(service)
        assertTrue(service.isAvailable() || service is NoOpSubtitleBurnInService)
    }
}
