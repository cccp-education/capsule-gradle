package capsule.scenarios

import capsule.ProcessRunner
import capsule.ScreenshotPlanner
import capsule.VideoFormatConverterImpl
import java.io.File
import java.util.Base64

/**
 * Test support: produces a **real** short-form MP4 through the FFmpeg socle,
 * without Chromium (CAP-SHORT US-5).
 *
 * It replays exactly the socle steps `ScreenshotCaptureImpl` runs once the PNGs
 * exist — [ScreenshotPlanner] PNG→WebM per slide, concat demuxer, then
 * [VideoFormatConverterImpl] WebM→MP4 — but feeds a locally generated PNG so the
 * scenario needs only `ffmpeg`/`ffprobe` (installed in the Full Tests job), not a
 * browser. This proves the viral render really writes an exploitable MP4 whose
 * duration matches the fitted target.
 */
object ViralRealRenderSupport {

    /** A valid 1×1 PNG; ffmpeg scales it to the portrait viewport. */
    private const val ONE_PIXEL_PNG_BASE64 =
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

    /**
     * Renders a real MP4 of [durations] into [outputDir] via ffmpeg.
     *
     * @throws IllegalStateException when any ffmpeg step fails.
     */
    fun renderMp4(
        outputDir: File,
        durations: List<Double>,
        viewportWidth: Int,
        viewportHeight: Int,
        ffmpegPath: String = "ffmpeg",
    ): File {
        val plan = ScreenshotPlanner.plan(outputDir, durations)
        outputDir.mkdirs()
        val png = Base64.getDecoder().decode(ONE_PIXEL_PNG_BASE64)

        plan.slides.forEach { entry ->
            entry.pngFile.writeBytes(png)
            val encoded = ProcessRunner.run(
                ScreenshotPlanner.ffmpegPngToWebmArgs(entry, viewportWidth, viewportHeight, ffmpegPath),
            )
            check(encoded.isSuccess) { "ffmpeg png->webm failed (slide ${entry.index}): ${encoded.output}" }
        }

        plan.concatListFile.writeText(ScreenshotPlanner.renderConcatList(plan))
        val concat = ProcessRunner.run(ScreenshotPlanner.ffmpegConcatArgs(plan, ffmpegPath))
        check(concat.isSuccess) { "ffmpeg concat failed: ${concat.output}" }

        val mp4 = File(outputDir, "capsule.mp4")
        val converted = VideoFormatConverterImpl(ffmpegPath).convertToMp4(plan.finalWebm, mp4)
        check(converted) { "webm->mp4 conversion failed" }
        return mp4
    }
}
