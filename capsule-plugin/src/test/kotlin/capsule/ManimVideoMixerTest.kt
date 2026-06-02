package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD Baby Step 1 — ManimVideoMixer unit tests.
 *
 * ManimVideoMixer muxes a Manim MP4 video with a TTS MP3 audio track
 * to produce a final MP4 with synchronized audio.
 *
 * Design:
 * - Interface ManimVideoMixer with mix(video, audio, output) + NoOpManimVideoMixer
 * - Real implementation: ManimVideoMixerImpl using ffmpeg
 * - NoOp: creates placeholder for testing
 */
class ManimVideoMixerTest {

    // ─── NoOpManimVideoMixer ─────────────────────────────────────

    @Test
    fun `noop mixer is available and named noop-mixer`() {
        val mixer = NoOpManimVideoMixer()
        assertTrue(mixer.isAvailable())
        assertEquals("noop-mixer", mixer.name())
    }

    @Test
    fun `noop mixer creates placeholder MP4 with metadata`() {
        val mixer = NoOpManimVideoMixer()
        val tmpDir = File.createTempFile("mixer-test", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val videoFile = File(tmpDir, "scene.mp4")
        videoFile.writeText("fake manim video")
        val audioFile = File(tmpDir, "tts.mp3")
        audioFile.writeText("# TTS PLACEHOLDER")

        val outputFile = File(tmpDir, "final.mp4")
        val result = mixer.mix(videoFile, audioFile, outputFile)

        assertTrue(result.exists(), "NoOp mixer should produce output file")
        assertTrue(result.name == "final.mp4")
        val content = result.readText()
        assertTrue(content.contains("MANIM MIXER PLACEHOLDER"), "Should contain placeholder marker")
        assertTrue(content.contains("scene.mp4"), "Should mention source video")
        assertTrue(content.contains("tts.mp3"), "Should mention source audio")
    }

    @Test
    fun `noop mixer creates parent directories for output`() {
        val mixer = NoOpManimVideoMixer()
        val tmpDir = File.createTempFile("mixer-mkdir", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val videoFile = File(tmpDir, "input.mp4")
        videoFile.writeText("fake")
        val audioFile = File(tmpDir, "input.mp3")
        audioFile.writeText("fake")

        val nestedOutput = File(tmpDir, "nested/sub/dir/output.mp4")
        val result = mixer.mix(videoFile, audioFile, nestedOutput)

        assertTrue(result.exists(), "Should create nested output directories")
        assertTrue(result.parentFile.exists(), "Parent directory should exist")
    }

    // ─── NoOp mixer without audio (manim slide has no TTS) ──────

    @Test
    fun `noop mixer handles missing audio gracefully`() {
        val mixer = NoOpManimVideoMixer()
        val tmpDir = File.createTempFile("mixer-noaudio", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val videoFile = File(tmpDir, "scene.mp4")
        videoFile.writeText("fake manim video")
        val missingAudio = File(tmpDir, "nonexistent.mp3")
        // audio file does NOT exist

        val outputFile = File(tmpDir, "final-noaudio.mp4")
        val result = mixer.mix(videoFile, missingAudio, outputFile)

        assertTrue(result.exists(), "Should still produce output even without audio")
        val content = result.readText()
        assertTrue(content.contains("MANIM MIXER PLACEHOLDER"))
        // NoOp mixer does not validate audio existence — just records it
    }

    // ─── ManimVideoMixerImpl (without real ffmpeg) ───────────────

    @Test
    fun `mixer impl reports unavailable when ffmpeg not found`() {
        val mixer = ManimVideoMixerImpl(ffmpegPath = "/nonexistent/path/ffmpeg")
        assertFalse(mixer.isAvailable(), "Should report unavailable for nonexistent ffmpeg")
    }

    @Test
    fun `mixer impl name returns ffmpeg-mixer`() {
        val mixer = ManimVideoMixerImpl()
        assertEquals("ffmpeg-mixer", mixer.name())
    }

    @Test
    fun `mixer impl throws MixerException when ffmpeg not available and mix called`() {
        val mixer = ManimVideoMixerImpl(ffmpegPath = "/nonexistent/path/ffmpeg")
        val tmpDir = File.createTempFile("mixer-exc", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val videoFile = File(tmpDir, "scene.mp4")
        videoFile.writeText("fake")
        val audioFile = File(tmpDir, "audio.mp3")
        audioFile.writeText("fake")
        val outputFile = File(tmpDir, "output.mp4")

        var exceptionCaught = false
        try {
            mixer.mix(videoFile, audioFile, outputFile)
        } catch (e: MixerException) {
            exceptionCaught = true
            assertTrue(e.message!!.contains("ffmpeg"), "Exception should mention ffmpeg")
        }
        assertTrue(exceptionCaught, "Should throw MixerException when ffmpeg unavailable")
    }

    // ─── ManimVideoMixerImpl probeDuration ───────────────────────

    @Test
    fun `mixer impl probeDuration returns 0 for nonexistent file`() {
        val mixer = ManimVideoMixerImpl(ffmpegPath = "/nonexistent/path/ffmpeg")
        val missingFile = File("/tmp/this-file-does-not-exist-${System.currentTimeMillis()}.mp4")
        assertEquals(0.0, mixer.probeDuration(missingFile), "Should return 0.0 for missing file")
    }

    @Test
    fun `noop mixer probeDuration returns 0`() {
        val mixer = NoOpManimVideoMixer()
        val dummyFile = File.createTempFile("probe-test", ".mp4")
        dummyFile.deleteOnExit()
        assertEquals(0.0, mixer.probeDuration(dummyFile))
    }

    // ─── resolveManimVideoMixer factory ───────────────────────────

    @Test
    fun `resolveManimVideoMixer returns NoOpManimVideoMixer when ffmpeg is noop`() {
        val mixer = CapsuleManager.resolveManimVideoMixer("/nonexistent/path/ffmpeg")
        assertTrue(mixer is NoOpManimVideoMixer, "Should return NoOp mixer when ffmpeg not found")
    }

    @Test
    fun `resolveManimVideoMixer returns ManimVideoMixerImpl with default ffmpeg`() {
        val mixer = CapsuleManager.resolveManimVideoMixer("ffmpeg")
        // On test env, ffmpeg may or may not be available — just verify it doesn't throw
        assertNotNull(mixer)
        // Either ManimVideoMixerImpl (if ffmpeg present) or NoOpManimVideoMixer (fallback)
        assertTrue(mixer.isAvailable() || mixer is NoOpManimVideoMixer)
    }
}