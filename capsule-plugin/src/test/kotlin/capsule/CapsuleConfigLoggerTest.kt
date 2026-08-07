package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for [CapsuleConfigLogger.formatConfigLog].
 *
 * CR-8 — Logging structuré : the monolithic 13-placeholder log line is decomposed
 * into 4 section lines (TTS, Capture, Subtitles, Manim). Tokens searched by
 * existing Cucumber steps (`engine=`, `quality=`, `subtitle=`, `subtitleFormat=`,
 * `burnIn=`, `scriptsDir=`) MUST be preserved verbatim.
 */
class CapsuleConfigLoggerTest {

    @Test
    fun `formatConfigLog returns 4 lines for default config`() {
        val lines = CapsuleConfigLogger.formatConfigLog(CapsuleConfig())
        assertEquals(4, lines.size, "Should produce exactly 4 section lines")
    }

    @Test
    fun `line 0 is TTS section with engine voice language tokens`() {
        val config = CapsuleConfig(tts = TtsConfig(engine = "espeak", voice = "fr", language = "en"))
        val line = CapsuleConfigLogger.formatConfigLog(config)[0]
        assertTrue(line.contains("TTS"), "Line 0 should be TTS section: $line")
        assertTrue(line.contains("engine=espeak"), "Should preserve engine= token: $line")
        assertTrue(line.contains("voice=fr"), "Should preserve voice= token: $line")
        assertTrue(line.contains("language=en"), "Should preserve language= token: $line")
    }

    @Test
    fun `line 1 is Capture section with vw parallel tokens`() {
        val config = CapsuleConfig(capture = CaptureConfig(viewportWidth = 1920, viewportHeight = 1080, parallelCaptureEnabled = true))
        val line = CapsuleConfigLogger.formatConfigLog(config)[1]
        assertTrue(line.contains("Capture"), "Line 1 should be Capture section: $line")
        assertTrue(line.contains("vw=1920x1080"), "Should preserve vw= token: $line")
        assertTrue(line.contains("parallel=true"), "Should preserve parallel= token: $line")
    }

    @Test
    fun `line 2 is Subtitles section with subtitle subtitleFormat burnIn tokens`() {
        val config = CapsuleConfig(capture = CaptureConfig(subtitleEnabled = true, subtitleFormat = "vtt", subtitleBurnIn = true))
        val line = CapsuleConfigLogger.formatConfigLog(config)[2]
        assertTrue(line.contains("Subtitle"), "Line 2 should be Subtitle section: $line")
        assertTrue(line.contains("subtitle=true"), "Should preserve subtitle= token: $line")
        assertTrue(line.contains("subtitleFormat=vtt"), "Should preserve subtitleFormat= token: $line")
        assertTrue(line.contains("burnIn=true"), "Should preserve burnIn= token: $line")
    }

    @Test
    fun `line 3 is Manim section with path quality scriptsDir outputDir tokens`() {
        val config = CapsuleConfig(manim = ManimConfig(executablePath = "/usr/bin/manim", quality = "h", scriptsDir = "custom/manim", outputDir = "out"))
        val line = CapsuleConfigLogger.formatConfigLog(config)[3]
        assertTrue(line.contains("Manim"), "Line 3 should be Manim section: $line")
        assertTrue(line.contains("path=/usr/bin/manim"), "Should preserve path= token: $line")
        assertTrue(line.contains("quality=h"), "Should preserve quality= token: $line")
        assertTrue(line.contains("scriptsDir=custom/manim"), "Should preserve scriptsDir= token: $line")
        assertTrue(line.contains("outputDir=out"), "Should preserve outputDir= token: $line")
    }

    @Test
    fun `default config line 0 contains engine=piper`() {
        val line = CapsuleConfigLogger.formatConfigLog(CapsuleConfig())[0]
        assertTrue(line.contains("engine=piper"), "Default TTS engine should be piper: $line")
    }

    @Test
    fun `default config line 2 contains subtitle=false`() {
        val line = CapsuleConfigLogger.formatConfigLog(CapsuleConfig())[2]
        assertTrue(line.contains("subtitle=false"), "Default subtitle should be false: $line")
    }

    @Test
    fun `default config line 3 contains quality=l`() {
        val line = CapsuleConfigLogger.formatConfigLog(CapsuleConfig())[3]
        assertTrue(line.contains("quality=l"), "Default manim quality should be l: $line")
    }

    @Test
    fun `all 4 lines are non-blank`() {
        val lines = CapsuleConfigLogger.formatConfigLog(CapsuleConfig())
        lines.forEachIndexed { idx, line ->
            assertTrue(line.isNotBlank(), "Line $idx should not be blank")
        }
    }
}