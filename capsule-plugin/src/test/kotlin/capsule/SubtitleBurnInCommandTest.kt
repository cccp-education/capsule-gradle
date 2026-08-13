package capsule

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * TDD Baby Step — CAP-27 US-1 — `SubtitleBurnInCommand` pure object.
 *
 * `SubtitleBurnInCommand` extracts the FFmpeg argv construction from
 * `SubtitleBurnInServiceImpl.burnIn` (lines 66-72) into a pure object
 * with no I/O, fully testable. Pattern de reference : `AudioPostCommand`
 * (CAP-AUDIO US-2) and `FormatConversion` (CAP-MP4 US-2).
 *
 * The argv is identical to the inline construction:
 *   [ffmpegPath, "-y", "-i", video, "-vf",
 *    "subtitles=<subtitle>:force_style='<style>'",
 *    "-c:a", "copy", output]
 *
 * CAP-27 verifies VTT compatibility : the `subtitles=` filter accepts a
 * `.vtt` file directly (FFmpeg auto-detects format by extension). This
 * pure object lets us assert the `.vtt` path is wired correctly without
 * running real FFmpeg.
 */
class SubtitleBurnInCommandTest {

    // ─── buildArgv SRT path ───────────────────────────────────────

    @Test
    fun `buildArgv SRT path passes the srt file to the subtitles filter`() {
        val video = File("/tmp/deck.webm")
        val subtitle = File("/tmp/deck.srt")
        val output = File("/tmp/burned.webm")
        val argv = SubtitleBurnInCommand.buildArgv(video, subtitle, output, SubtitleBurnInStyle(), "ffmpeg")
        val vf = argv[argv.indexOf("-vf") + 1]
        assertTrue(vf.startsWith("subtitles="), "filter should start with subtitles=")
        assertTrue(vf.contains("deck.srt"), "filter should reference the srt file")
        assertTrue(vf.endsWith("force_style='${SubtitleBurnInStyle().toForceStyle()}'"),
            "filter should end with the force_style segment")
    }

    @Test
    fun `buildArgv VTT path passes the vtt file to the subtitles filter`() {
        val video = File("/tmp/deck.webm")
        val subtitle = File("/tmp/deck.vtt")
        val output = File("/tmp/burned.webm")
        val argv = SubtitleBurnInCommand.buildArgv(video, subtitle, output, SubtitleBurnInStyle(), "ffmpeg")
        val vf = argv[argv.indexOf("-vf") + 1]
        assertTrue(vf.startsWith("subtitles="), "filter should start with subtitles=")
        assertTrue(vf.contains("deck.vtt"), "filter should reference the vtt file (CAP-27 core)")
    }

    @Test
    fun `buildArgv SRT and VTT differ only by the subtitle file path`() {
        val video = File("/tmp/deck.webm")
        val output = File("/tmp/burned.webm")
        val srtArgv = SubtitleBurnInCommand.buildArgv(video, File("/tmp/deck.srt"), output, SubtitleBurnInStyle(), "ffmpeg")
        val vttArgv = SubtitleBurnInCommand.buildArgv(video, File("/tmp/deck.vtt"), output, SubtitleBurnInStyle(), "ffmpeg")
        val srtVf = srtArgv[srtArgv.indexOf("-vf") + 1]
        val vttVf = vttArgv[vttArgv.indexOf("-vf") + 1]
        assertEquals(srtVf.replace("deck.srt", "deck.vtt"), vttVf,
            "only the subtitle extension should differ between SRT and VTT")
    }

    // ─── buildArgv structure (flags + order) ─────────────────────

    @Test
    fun `buildArgv starts with ffmpegPath and -y flag`() {
        val video = File("/tmp/deck.webm")
        val subtitle = File("/tmp/deck.srt")
        val output = File("/tmp/burned.webm")
        val argv = SubtitleBurnInCommand.buildArgv(video, subtitle, output, SubtitleBurnInStyle(), "/usr/bin/ffmpeg")
        assertEquals("/usr/bin/ffmpeg", argv[0], "first element should be the ffmpeg path")
        assertEquals("-y", argv[1], "second element should be the -y flag")
    }

    @Test
    fun `buildArgv has exactly one -i flag pointing at the video file`() {
        val video = File("/tmp/deck.webm")
        val subtitle = File("/tmp/deck.srt")
        val output = File("/tmp/burned.webm")
        val argv = SubtitleBurnInCommand.buildArgv(video, subtitle, output, SubtitleBurnInStyle(), "ffmpeg")
        assertEquals(1, argv.count { it == "-i" }, "should have exactly one -i flag")
        assertEquals(video.absolutePath, argv[argv.indexOf("-i") + 1], "-i should point at the video file")
    }

    @Test
    fun `buildArgv preserves audio with copy codec`() {
        val video = File("/tmp/deck.webm")
        val subtitle = File("/tmp/deck.vtt")
        val output = File("/tmp/burned.webm")
        val argv = SubtitleBurnInCommand.buildArgv(video, subtitle, output, SubtitleBurnInStyle(), "ffmpeg")
        val caIdx = argv.indexOf("-c:a")
        assertTrue(caIdx >= 0, "should contain -c:a flag")
        assertEquals("copy", argv[caIdx + 1], "-c:a should be copy (audio preserved)")
    }

    @Test
    fun `buildArgv ends with the output file absolute path`() {
        val video = File("/tmp/deck.webm")
        val subtitle = File("/tmp/deck.srt")
        val output = File("/tmp/out/burned.webm")
        val argv = SubtitleBurnInCommand.buildArgv(video, subtitle, output, SubtitleBurnInStyle(), "ffmpeg")
        assertEquals(output.absolutePath, argv.last(), "last element should be the output file absolute path")
    }

    @Test
    fun `buildArgv has the expected 9-element structure`() {
        val video = File("/tmp/deck.webm")
        val subtitle = File("/tmp/deck.srt")
        val output = File("/tmp/burned.webm")
        val argv = SubtitleBurnInCommand.buildArgv(video, subtitle, output, SubtitleBurnInStyle(), "ffmpeg")
        assertEquals(9, argv.size, "argv should have 9 elements: ffmpeg, -y, -i, video, -vf, filter, -c:a, copy, output")
    }

    // ─── force_style propagation ──────────────────────────────────

    @Test
    fun `buildArgv injects default force_style into the subtitles filter`() {
        val video = File("/tmp/deck.webm")
        val subtitle = File("/tmp/deck.srt")
        val output = File("/tmp/burned.webm")
        val argv = SubtitleBurnInCommand.buildArgv(video, subtitle, output, SubtitleBurnInStyle(), "ffmpeg")
        val vf = argv[argv.indexOf("-vf") + 1]
        assertTrue(vf.contains("FontSize=24"), "default style should have FontSize=24")
        assertTrue(vf.contains("PrimaryColour=&H00FFFFFF"), "default style should have white primary colour")
        assertTrue(vf.contains("Alignment=2"), "default bottom position should map to Alignment=2")
    }

    @Test
    fun `buildArgv injects custom style into the subtitles filter`() {
        val video = File("/tmp/deck.webm")
        val subtitle = File("/tmp/deck.vtt")
        val output = File("/tmp/burned.webm")
        val style = SubtitleBurnInStyle(fontSize = 36, fontColor = "&H000000FF", outlineColor = "&H00FF0000", position = "top")
        val argv = SubtitleBurnInCommand.buildArgv(video, subtitle, output, style, "ffmpeg")
        val vf = argv[argv.indexOf("-vf") + 1]
        assertTrue(vf.contains("FontSize=36"), "custom style should propagate fontSize=36")
        assertTrue(vf.contains("PrimaryColour=&H000000FF"), "custom style should propagate fontColor")
        assertTrue(vf.contains("OutlineColour=&H00FF0000"), "custom style should propagate outlineColor")
        assertTrue(vf.contains("Alignment=8"), "top position should map to Alignment=8")
    }

    @Test
    fun `buildArgv top position maps to Alignment 8`() {
        val argv = SubtitleBurnInCommand.buildArgv(
            File("/tmp/v.webm"), File("/tmp/v.srt"), File("/tmp/o.webm"),
            SubtitleBurnInStyle(position = "top"), "ffmpeg")
        assertTrue(argv[argv.indexOf("-vf") + 1].contains("Alignment=8"))
    }

    @Test
    fun `buildArgv middle position maps to Alignment 5`() {
        val argv = SubtitleBurnInCommand.buildArgv(
            File("/tmp/v.webm"), File("/tmp/v.vtt"), File("/tmp/o.webm"),
            SubtitleBurnInStyle(position = "middle"), "ffmpeg")
        assertTrue(argv[argv.indexOf("-vf") + 1].contains("Alignment=5"))
    }

    @Test
    fun `buildArgv propagates custom ffmpegPath as first element`() {
        val argv = SubtitleBurnInCommand.buildArgv(
            File("/tmp/v.webm"), File("/tmp/v.srt"), File("/tmp/o.webm"),
            SubtitleBurnInStyle(), "/custom/path/ffmpeg")
        assertEquals("/custom/path/ffmpeg", argv[0])
    }
}