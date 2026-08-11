package capsule

import capsule.audio.AudioPostCommand
import capsule.audio.AudioPostConfig
import capsule.audio.AudioPostException
import capsule.audio.AudioPostProcessor
import capsule.audio.AudioPostProcessorImpl
import capsule.audio.NoOpAudioPostProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.io.File

/**
 * TDD unit tests for CAP-AUDIO US-2 — `AudioPostProcessor` (interface +
 * Impl + NoOp + Exception) + `AudioPostCommand` (pure object) +
 * `CapsuleManager.resolveAudioPostProcessor` factory (5ème).
 *
 * Pattern de référence : [VideoFormatConverter] + [FormatConversion]
 * (CAP-MP4 US-2) et [SubtitleBurnInService] (interface + Impl + NoOp +
 * factory + Exception).
 *
 * `AudioPostCommand` is a pure object — `buildFilterGraph` and `buildArgv`
 * have no I/O, fully testable. `AudioPostProcessorImpl` wraps FFmpeg
 * (loudnorm EBU R128 + amix BGM + sidechaincompress ducking).
 * `NoOpAudioPostProcessor` is the test fallback (returns `false` — caller
 * keeps the original video, degraded mode backward compat).
 */
class AudioPostProcessorTest {

    // ─── AudioPostCommand.buildFilterGraph (pure, no I/O) ─────────

    @Test
    fun `buildFilterGraph loudness-only when bgmDisabled and duckingDisabled`() {
        val config = AudioPostConfig(bgmEnabled = false, duckingEnabled = false, loudnessTarget = -16.0)
        val graph = AudioPostCommand.buildFilterGraph(config, hasBgmInput = false)
        assertTrue(graph.contains("loudnorm=I=-16.0:TP=-1.5:LRA=11"), "should apply loudnorm")
        assertFalse(graph.contains("amix"), "no BGM mix when bgmDisabled")
        assertFalse(graph.contains("sidechaincompress"), "no ducking when duckingDisabled")
    }

    @Test
    fun `buildFilterGraph loudness plus BGM when bgmEnabled and duckingDisabled`() {
        val config = AudioPostConfig(bgmEnabled = true, bgmLevel = -18.0, loudnessTarget = -16.0, duckingEnabled = false)
        val graph = AudioPostCommand.buildFilterGraph(config, hasBgmInput = true)
        assertTrue(graph.contains("loudnorm=I=-16.0:TP=-1.5:LRA=11"), "should apply loudnorm")
        assertTrue(graph.contains("volume=-18.0dB"), "should apply BGM volume")
        assertTrue(graph.contains("amix=inputs=2"), "should mix BGM with voice")
        assertFalse(graph.contains("sidechaincompress"), "no ducking when duckingDisabled")
    }

    @Test
    fun `buildFilterGraph loudness plus BGM plus ducking when both enabled`() {
        val config = AudioPostConfig(bgmEnabled = true, bgmLevel = -18.0, loudnessTarget = -16.0, duckingEnabled = true)
        val graph = AudioPostCommand.buildFilterGraph(config, hasBgmInput = true)
        assertTrue(graph.contains("loudnorm=I=-16.0:TP=-1.5:LRA=11"), "should apply loudnorm")
        assertTrue(graph.contains("sidechaincompress"), "should apply ducking")
        assertTrue(graph.contains("amix=inputs=2"), "should mix BGM with voice")
    }

    @Test
    fun `buildFilterGraph no-BGM no-ducking returns loudness-only filter`() {
        val config = AudioPostConfig(bgmEnabled = false, duckingEnabled = false, loudnessTarget = -14.0)
        val graph = AudioPostCommand.buildFilterGraph(config, hasBgmInput = false)
        assertTrue(graph.contains("loudnorm=I=-14.0:TP=-1.5:LRA=11"), "should apply loudnorm with configured target")
        assertFalse(graph.contains("amix"), "no BGM mix without BGM")
        assertFalse(graph.contains("sidechaincompress"), "no ducking without ducking")
    }

    @Test
    fun `buildFilterGraph ducking without BGM is ignored (loudness-only fallback)`() {
        val config = AudioPostConfig(bgmEnabled = false, duckingEnabled = true, loudnessTarget = -16.0)
        val graph = AudioPostCommand.buildFilterGraph(config, hasBgmInput = false)
        assertTrue(graph.contains("loudnorm"), "should apply loudnorm")
        assertFalse(graph.contains("sidechaincompress"), "ducking without BGM should be ignored")
        assertFalse(graph.contains("amix"), "no mix without BGM")
    }

    // ─── AudioPostCommand.buildArgv (pure, no I/O) ────────────────

    @Test
    fun `buildArgv includes second -i input for BGM file when BGM enabled`() {
        val config = AudioPostConfig(bgmEnabled = true, bgmFile = "/music/loop.mp3", loudnessTarget = -16.0)
        val video = File("/tmp/deck.webm")
        val bgm = File("/music/loop.mp3")
        val output = File("/tmp/out.webm")
        val argv = AudioPostCommand.buildArgv(video, bgm, output, config, "ffmpeg")
        assertTrue(argv.contains("-i"), "should have -i inputs")
        assertTrue(argv.contains("/music/loop.mp3"), "should include BGM file as input")
        assertTrue(argv.contains("-filter_complex"), "should use filter_complex for audio post")
    }

    @Test
    fun `buildArgv does not include BGM input when BGM disabled`() {
        val config = AudioPostConfig(bgmEnabled = false, loudnessTarget = -16.0)
        val video = File("/tmp/deck.webm")
        val output = File("/tmp/out.webm")
        val argv = AudioPostCommand.buildArgv(video, null, output, config, "ffmpeg")
        assertEquals(1, argv.count { it == "-i" }, "should have exactly 1 -i flag (video only) when no BGM")
        assertFalse(argv.contains("-filter_complex"), "no filter_complex needed for loudness-only (uses -af)")
    }

    // ─── AudioPostProcessorImpl (without real ffmpeg) ─────────────

    @Test
    fun `AudioPostProcessorImpl with bogus path reports unavailable`() {
        val processor = AudioPostProcessorImpl(ffmpegPath = "/nonexistent/ffmpeg/path_xyz")
        assertFalse(processor.isAvailable(), "Should report unavailable for nonexistent ffmpeg")
    }

    @Test
    fun `AudioPostProcessorImpl name returns ffmpeg-audio-post`() {
        val processor = AudioPostProcessorImpl()
        assertEquals("ffmpeg-audio-post", processor.name())
    }

    @Test
    fun `AudioPostProcessorImpl process returns false when ffmpeg unavailable`() {
        val processor = AudioPostProcessorImpl(ffmpegPath = "/nonexistent/ffmpeg/path_xyz")
        val video = File.createTempFile("deck", ".webm").apply { writeText("dummy") }
        val output = File.createTempFile("out", ".webm")
        try {
            val config = AudioPostConfig(bgmEnabled = false, loudnessTarget = -16.0)
            val result = processor.process(video, output, config)
            assertFalse(result, "Should return false when ffmpeg unavailable (degraded)")
        } finally {
            video.delete(); output.delete()
        }
    }

    @Test
    fun `AudioPostProcessorImpl process returns false when BGM enabled but file blank (degraded)`() {
        val processor = AudioPostProcessorImpl(ffmpegPath = "/nonexistent/ffmpeg/path_xyz")
        val video = File.createTempFile("deck", ".webm").apply { writeText("dummy") }
        val output = File.createTempFile("out", ".webm")
        try {
            val config = AudioPostConfig(bgmEnabled = true, bgmFile = "", loudnessTarget = -16.0)
            val result = processor.process(video, output, config)
            assertFalse(result, "Should return false when BGM enabled but file blank (degraded, keep original)")
        } finally {
            video.delete(); output.delete()
        }
    }

    // ─── NoOpAudioPostProcessor ───────────────────────────────────

    @Test
    fun `NoOpAudioPostProcessor is available and named noop-audio-post`() {
        val processor = NoOpAudioPostProcessor()
        assertTrue(processor.isAvailable(), "NoOp is always available (fallback)")
        assertEquals("noop-audio-post", processor.name())
    }

    @Test
    fun `NoOpAudioPostProcessor process returns false (degraded, keep original)`() {
        val processor = NoOpAudioPostProcessor()
        val video = File.createTempFile("deck", ".webm").apply { writeText("dummy") }
        val output = File.createTempFile("out", ".webm")
        try {
            val config = AudioPostConfig(bgmEnabled = true, loudnessTarget = -16.0)
            val result = processor.process(video, output, config)
            assertFalse(result, "NoOp should return false (no real audio post, keep original)")
        } finally {
            video.delete(); output.delete()
        }
    }

    // ─── resolveAudioPostProcessor factory ────────────────────────

    @Test
    fun `resolveAudioPostProcessor with ffmpegPath noop returns NoOp`() {
        val resolved = CapsuleManager.resolveAudioPostProcessor(ffmpegPath = "noop", strict = false)
        assertTrue(resolved is NoOpAudioPostProcessor, "noop path should return NoOp")
        assertEquals("noop-audio-post", resolved.name())
    }

    @Test
    fun `resolveAudioPostProcessor unavailable and non-strict falls back to NoOp`() {
        val resolved = CapsuleManager.resolveAudioPostProcessor(
            ffmpegPath = "/nonexistent/ffmpeg/path_xyz",
            strict = false
        )
        assertTrue(resolved is NoOpAudioPostProcessor, "unavailable + non-strict → NoOp fallback")
    }

    @Test
    fun `resolveAudioPostProcessor unavailable and strict throws IllegalStateException`() {
        val ex = assertFailsWith<IllegalStateException> {
            CapsuleManager.resolveAudioPostProcessor(
                ffmpegPath = "/nonexistent/ffmpeg/path_xyz",
                strict = true
            )
        }
        assertTrue(ex.message!!.contains("ffmpeg"), "error should name ffmpeg")
        assertTrue(ex.message!!.contains("/nonexistent/ffmpeg/path_xyz"), "error should mention the path")
        assertTrue(ex.message!!.contains("strictMode"), "error should suggest disabling strictMode")
    }

    @Test
    fun `resolveAudioPostProcessor named ffmpeg-audio-post when impl returned`() {
        val impl = AudioPostProcessorImpl("ffmpeg")
        if (impl.isAvailable()) {
            val resolved = CapsuleManager.resolveAudioPostProcessor("ffmpeg", strict = false)
            assertEquals("ffmpeg-audio-post", resolved.name())
        }
    }
}