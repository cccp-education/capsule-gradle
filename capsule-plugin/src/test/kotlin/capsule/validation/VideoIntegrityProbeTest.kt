package capsule.validation

import capsule.ProcessRunner
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Unit tests for [VideoIntegrityProbe] (PRT-04) driven by
 * [ProcessRunner.commandInterceptor]: no real ffmpeg is spawned, the exact
 * decode argv is captured, and the raw outcome is mapped through
 * [VideoIntegrityChecker].
 */
class VideoIntegrityProbeTest {

    @TempDir
    lateinit var tempDir: File

    private val captured = mutableListOf<List<String>>()

    @AfterEach
    fun tearDown() {
        ProcessRunner.commandInterceptor = null
    }

    private fun intercept(exitCode: Int, output: String) {
        captured.clear()
        ProcessRunner.commandInterceptor = { command, _, _, _, _ ->
            captured.add(command)
            ProcessRunner.Result(exitCode, output)
        }
    }

    private fun clip(name: String = "clip.webm"): File =
        File(tempDir, name).also { it.writeText("x") }

    @Test
    fun `delegates a null-muxer decode and maps success to clean`() {
        intercept(0, "")
        val file = clip()

        val verdict = VideoIntegrityProbe.probe(file, ffmpegPath = "ffmpeg")

        assertThat(verdict).isEqualTo(IntegrityResult.Clean)
        val cmd = captured.single()
        assertThat(cmd.first()).isEqualTo("ffmpeg")
        assertThat(cmd).containsSubsequence("-v", "error")
        assertThat(cmd).containsSubsequence("-i", file.absolutePath)
        assertThat(cmd).containsSubsequence("-f", "null", "-")
    }

    @Test
    fun `maps a decoder failure to corrupt with the diagnostic`() {
        intercept(1, "moov atom not found")

        val verdict = VideoIntegrityProbe.probe(clip(), ffmpegPath = "ffmpeg")

        assertThat(verdict).isInstanceOf(IntegrityResult.Corrupt::class.java)
        assertThat((verdict as IntegrityResult.Corrupt).stderrTail).isEqualTo("moov atom not found")
    }

    @Test
    fun `missing file is corrupt without invoking the decoder`() {
        intercept(0, "")
        val missing = File(tempDir, "absent.webm")

        val verdict = VideoIntegrityProbe.probe(missing, ffmpegPath = "ffmpeg")

        assertThat(verdict).isInstanceOf(IntegrityResult.Corrupt::class.java)
        assertThat((verdict as IntegrityResult.Corrupt).stderrTail).contains("missing")
        assertThat(captured).isEmpty()
    }

    @Test
    fun `empty file is corrupt without invoking the decoder`() {
        intercept(0, "")
        val empty = File(tempDir, "empty.webm").also { it.writeText("") }

        val verdict = VideoIntegrityProbe.probe(empty, ffmpegPath = "ffmpeg")

        assertThat(verdict).isInstanceOf(IntegrityResult.Corrupt::class.java)
        assertThat(captured).isEmpty()
    }
}
