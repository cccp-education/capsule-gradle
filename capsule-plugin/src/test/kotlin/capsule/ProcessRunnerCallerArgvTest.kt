package capsule

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import capsule.audio.AudioPostConfig
import capsule.audio.AudioPostProcessorImpl
import capsule.podcast.PodcastConcatenatorImpl
import capsule.SubtitleBurnInServiceImpl
import capsule.SubtitleBurnInStyle

/**
 * CAP-PR2-COV US-2 — argv + stdout redirection of the ffmpeg/npm/node callers
 * migrated to [ProcessRunner] (PR #2).
 *
 * Each caller is invoked with [ProcessRunner.commandInterceptor] installed: no
 * real process is spawned, the exact argv is captured, and a canned success
 * result is returned. The assertions prove the caller delegates to
 * ProcessRunner (argv prefixed by the binary) with the expected filter tokens,
 * and that a failing result is propagated (degraded/false) rather than
 * swallowed. Together with [capsule.ci.ProcessMigrationGuardTest] this locks
 * the migration end-to-end.
 */
class ProcessRunnerCallerArgvTest {

    private val captured = mutableListOf<List<String>>()

    @AfterEach
    fun tearDown() {
        ProcessRunner.commandInterceptor = null
    }

    private fun interceptSuccess() {
        captured.clear()
        ProcessRunner.commandInterceptor = { command, _, _, _, _ ->
            captured.add(command)
            ProcessRunner.Result(0, "")
        }
    }

    private fun temp(prefix: String): File =
        File(System.getProperty("java.io.tmpdir"), "${prefix}-${System.nanoTime()}").also { it.mkdirs() }

    private fun file(dir: File, name: String, content: String = "x"): File =
        File(dir, name).also { it.writeText(content) }

    /** Asserts every [token] appears as a substring of some argv element. */
    private fun argvContains(cmd: List<String>, vararg tokens: String) {
        tokens.forEach { token ->
            assertThat(cmd.any { it.contains(token) })
                .describedAs("argv $cmd must contain token '$token'")
                .isTrue()
        }
    }

    @Test
    fun `AudioPostProcessorImpl delegates to ProcessRunner with loudnorm argv`() {
        interceptSuccess()
        val dir = temp("audio-post")
        val video = file(dir, "in.webm")
        val out = file(dir, "out.webm")
        AudioPostProcessorImpl(ffmpegPath = "ffmpeg").process(video, out, AudioPostConfig())
        val cmd = captured.last()
        assertThat(cmd.first()).isEqualTo("ffmpeg")
        argvContains(cmd, video.absolutePath, out.absolutePath, "loudnorm")
    }

    @Test
    fun `AudioPostProcessorImpl propagates ffmpeg failure as degraded`() {
        captured.clear()
        ProcessRunner.commandInterceptor = { _, _, _, _, _ -> ProcessRunner.Result(1, "boom") }
        val dir = temp("audio-post-fail")
        val video = file(dir, "in.webm")
        val out = file(dir, "out.webm")
        assertThat(AudioPostProcessorImpl(ffmpegPath = "ffmpeg").process(video, out, AudioPostConfig())).isFalse()
    }

    @Test
    fun `SubtitleBurnInService delegates to ProcessRunner with subtitles argv`() {
        interceptSuccess()
        val dir = temp("burnin")
        val video = file(dir, "in.webm")
        val sub = file(dir, "sub.srt", "1\n00:00:00,000 --> 00:00:01,000\nhi\n")
        val out = file(dir, "out.webm")
        SubtitleBurnInServiceImpl(ffmpegPath = "ffmpeg", style = SubtitleBurnInStyle()).burnIn(video, sub, out)
        val cmd = captured.last()
        assertThat(cmd.first()).isEqualTo("ffmpeg")
        argvContains(cmd, video.absolutePath, sub.absolutePath, out.absolutePath, "subtitles")
    }

    @Test
    fun `VideoFormatConverter delegates to ProcessRunner with libx264 argv`() {
        interceptSuccess()
        val dir = temp("conv")
        val webm = file(dir, "in.webm")
        val mp4 = file(dir, "out.mp4")
        assertThat(VideoFormatConverterImpl(ffmpegPath = "ffmpeg").convertToMp4(webm, mp4)).isTrue()
        val cmd = captured.last()
        assertThat(cmd.first()).isEqualTo("ffmpeg")
        argvContains(cmd, webm.absolutePath, mp4.absolutePath, "libx264", "aac")
    }

    @Test
    fun `PodcastConcatenatorImpl delegates to ProcessRunner with concat argv`() {
        interceptSuccess()
        val dir = temp("podcast")
        val a = file(dir, "a.mp3")
        val b = file(dir, "b.mp3")
        val out = file(dir, "out.mp3")
        assertThat(PodcastConcatenatorImpl(ffmpegPath = "ffmpeg").concatenate(listOf(a, b), out)).isTrue()
        val cmd = captured.last()
        assertThat(cmd.first()).isEqualTo("ffmpeg")
        argvContains(cmd, "concat", out.absolutePath)
    }

    @Test
    fun `ManimVideoMixer delegates to ProcessRunner with both streams`() {
        interceptSuccess()
        val dir = temp("mix")
        val video = file(dir, "manim.mp4")
        val audio = file(dir, "tts.mp3")
        val out = file(dir, "mixed.mp4")
        ManimVideoMixerImpl(ffmpegPath = "ffmpeg").mix(video, audio, out)
        val cmd = captured.last()
        assertThat(cmd.first()).isEqualTo("ffmpeg")
        argvContains(cmd, video.absolutePath, audio.absolutePath, out.absolutePath, "-map")
    }

    @Test
    fun `MediaProbeUtil delegates to ProcessRunner and parses ffprobe output`() {
        captured.clear()
        ProcessRunner.commandInterceptor = { command, _, _, _, _ ->
            captured.add(command)
            ProcessRunner.Result(0, "12.5\n")
        }
        val dir = temp("probe")
        val media = file(dir, "clip.mp4")
        val duration = MediaProbeUtil.probeDuration(media, ffprobePath = "ffprobe")
        assertThat(duration).isEqualTo(12.5)
        val cmd = captured.last()
        assertThat(cmd.first()).isEqualTo("ffprobe")
        argvContains(cmd, media.absolutePath, "duration")
    }

    @Test
    fun `AudioConversionUtil delegates to ProcessRunner with libmp3lame argv`() {
        interceptSuccess()
        val dir = temp("wav")
        val wav = file(dir, "in.wav")
        val mp3 = File(dir, "out.mp3")
        AudioConversionUtil.wavToMp3(wav, mp3, ffmpegPath = "ffmpeg")
        val cmd = captured.last()
        assertThat(cmd.first()).isEqualTo("ffmpeg")
        argvContains(cmd, wav.absolutePath, mp3.absolutePath, "libmp3lame")
    }

    @Test
    fun `RemotionCapture delegates to ProcessRunner with the node render argv`() {
        interceptSuccess()
        val dir = temp("remotion")
        val deck = file(
            dir,
            "deck.html",
            """<html><head></head><body><div class="reveal"><div class="slides">""" +
                """<section>one</section><section>two</section>""" +
                """</div></div></body></html>""",
        )
        val outputDir = File(dir, "out").also { it.mkdirs() }
        // capture() asserts the rendered file exists once the (intercepted) render succeeds.
        file(outputDir, "capsule.mp4")
        RemotionCaptureImpl(projectDir = dir, nodeExecutablePath = "node")
            .capture(deck.absolutePath, outputDir, 1920, 1080, listOf(30.0, 30.0))
        val nodeCmd = captured.first { it.first() == "node" }
        argvContains(nodeCmd, "render.mjs", "--out", "--concurrency", "--codec")
        // npm install (ensureDependencies) is also routed through ProcessRunner
        assertThat(captured.any { it.first() == "npm" }).isTrue()
    }
}
