package capsule

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * TDD Baby Step — ManimVideoMixer + computeSlideDurationsWithManim unit tests.
 *
 * ManimVideoMixer muxes a Manim MP4 video with a TTS MP3 audio track
 * to produce a final MP4 with synchronized audio.
 *
 * computeSlideDurationsWithManim uses Manim-probed video durations
 * when available, falling back to TTS audio duration or default slide duration.
 *
 * Design:
 * - Interface ManimVideoMixer with mix(video, audio, output) + NoOpManimVideoMixer
 * - Real implementation: ManimVideoMixerImpl using ffmpeg
 * - NoOp: creates placeholder for testing
 */
class ComputeSlideDurationsWithManimTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTask(
        slideDurationSeconds: Double = 5.0
    ): CapsuleVideoTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val ext = CapsuleExtension(project.objects)
        ext.slideDurationSeconds.set(slideDurationSeconds)
        ext.ttsEngine.set("noop")
        ext.outputDir.set("capsule")

        val t = project.tasks.register("generateCapsuleVideo", CapsuleVideoTask::class.java).get()
        t.capsuleExtension = ext
        return t
    }

    @Test
    fun `computeSlideDurations uses manim duration when MP4 muxed file exists`() {
        val task = createTask(slideDurationSeconds = 5.0)
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        // Create a fake MP3 for slide 1 (HTML slide)
        File(audioDir, "slide-01.mp3").writeText("fake mp3 not probed")

        val script = CapsuleScript("test", listOf(
            SlideSegment(1, "Intro", "Note HTML", type = SlideType.HTML),
            SlideSegment(2, "Anim", "Note Manim", type = SlideType.MANIM, manimScene = "Scene1")
        ))

        // Manim slide 2 has a muxed MP4 with duration 8.5 seconds
        val manimDurations = mapOf(2 to 8.5)
        val durations = task.computeSlideDurationsWithManim(script, audioDir, manimDurations)

        // Slide 1 (HTML) should use default since MP3 cannot be probed by ffprobe
        assertEquals(5.0, durations[0], "HTML slide without probed MP3 should use default")
        // Slide 2 (MANIM) should use the manim duration
        assertEquals(8.5, durations[1], "MANIM slide should use probed manim duration")
    }

    @Test
    fun `computeSlideDurations falls back to default when manim duration is 0`() {
        val task = createTask(slideDurationSeconds = 5.0)
        val audioDir = File(tempDir, "audio2").also { it.mkdirs() }

        val script = CapsuleScript("test2", listOf(
            SlideSegment(1, "Intro", "Note", type = SlideType.HTML),
            SlideSegment(2, "Anim", "Note", type = SlideType.MANIM, manimScene = "Scene1")
        ))

        // Manim duration is 0.0 (probe failed)
        val manimDurations = mapOf(2 to 0.0)
        val durations = task.computeSlideDurationsWithManim(script, audioDir, manimDurations)

        assertEquals(5.0, durations[1], "Should fall back to default when manim duration is 0.0")
    }

    @Test
    fun `computeSlideDurations uses manim duration over default for all MANIM slides`() {
        val task = createTask(slideDurationSeconds = 5.0)
        val audioDir = File(tempDir, "audio3").also { it.mkdirs() }

        val script = CapsuleScript("test3", listOf(
            SlideSegment(1, "Intro", "Note", type = SlideType.HTML),
            SlideSegment(2, "Anim1", "Note", type = SlideType.MANIM, manimScene = "Scene1"),
            SlideSegment(3, "Anim2", "Note", type = SlideType.MANIM, manimScene = "Scene2"),
            SlideSegment(4, "End", "Note", type = SlideType.HTML)
        ))

        val manimDurations = mapOf(2 to 12.0, 3 to 7.5)
        val durations = task.computeSlideDurationsWithManim(script, audioDir, manimDurations)

        assertEquals(5.0, durations[0], "HTML slide 1 should use default")
        assertEquals(12.0, durations[1], "MANIM slide 2 should use probed duration")
        assertEquals(7.5, durations[2], "MANIM slide 3 should use probed duration")
        assertEquals(5.0, durations[3], "HTML slide 4 should use default")
    }

    @Test
    fun `computeSlideDurations with empty manim durations uses defaults for all`() {
        val task = createTask(slideDurationSeconds = 5.0)
        val audioDir = File(tempDir, "audio4").also { it.mkdirs() }

        val script = CapsuleScript("test4", listOf(
            SlideSegment(1, "Intro", "Note", type = SlideType.MANIM, manimScene = "Scene1"),
            SlideSegment(2, "End", "Note", type = SlideType.HTML)
        ))

        val manimDurations = emptyMap<Int, Double>()
        val durations = task.computeSlideDurationsWithManim(script, audioDir, manimDurations)

        assertEquals(5.0, durations[0], "MANIM slide without probe should use default")
        assertEquals(5.0, durations[1], "HTML slide should use default")
    }
}

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