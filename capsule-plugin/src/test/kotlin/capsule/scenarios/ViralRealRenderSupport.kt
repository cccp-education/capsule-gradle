package capsule.scenarios

import capsule.ProcessRunner
import capsule.ScreenshotPlanner
import capsule.VideoFormatConverterImpl
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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
 *
 * The slide PNG is generated at the **viewport size**: a 1×1 PNG makes ffmpeg's
 * `-loop 1` image demuxer hang, so we write a real 1080×1920 solid frame.
 */
object ViralRealRenderSupport {

    /** Per-ffmpeg-step guard, far below the task timeout (a hang fails fast). */
    private const val STEP_TIMEOUT_MINUTES: Long = 2L

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

        plan.slides.forEach { entry ->
            writeSolidPng(entry.pngFile, viewportWidth, viewportHeight)
            val encoded = ProcessRunner.run(
                ScreenshotPlanner.ffmpegPngToWebmArgs(entry, viewportWidth, viewportHeight, ffmpegPath),
                timeoutMinutes = STEP_TIMEOUT_MINUTES,
            )
            check(encoded.isSuccess) { "ffmpeg png->webm failed (slide ${entry.index}): ${encoded.tail()}" }
        }

        plan.concatListFile.writeText(ScreenshotPlanner.renderConcatList(plan))
        val concat = ProcessRunner.run(
            ScreenshotPlanner.ffmpegConcatArgs(plan, ffmpegPath),
            timeoutMinutes = STEP_TIMEOUT_MINUTES,
        )
        check(concat.isSuccess) { "ffmpeg concat failed: ${concat.tail()}" }

        val mp4 = File(outputDir, "capsule.mp4")
        val converted = VideoFormatConverterImpl(ffmpegPath).convertToMp4(plan.finalWebm, mp4)
        check(converted) { "webm->mp4 conversion failed" }
        return mp4
    }

    /** Writes a solid portrait PNG at the viewport size (a 1×1 PNG hangs ffmpeg). */
    private fun writeSolidPng(file: File, width: Int, height: Int) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color(15, 27, 51)
            graphics.fillRect(0, 0, width, height)
        } finally {
            graphics.dispose()
        }
        ImageIO.write(image, "png", file)
    }
}
