package capsule

import java.io.File

/**
 * Pure builder for the FFmpeg argv of [SubtitleBurnInServiceImpl] (CAP-27 US-1).
 *
 * Pure object — no I/O, no Gradle dependencies, fully testable.
 *
 * Pattern de reference : [capsule.audio.AudioPostCommand] (CAP-AUDIO US-2)
 * and [FormatConversion] (CAP-MP4 US-2).
 *
 * [buildArgv] assembles the full FFmpeg command line for burning subtitles
 * into a WebM video via the `subtitles=` filter. The subtitle file (SRT or
 * VTT) is passed directly to the filter — FFmpeg auto-detects the format
 * by extension, so VTT is supported without any format-specific branch
 * (CAP-27 core finding : VTT compatibility is already functional).
 *
 * The argv is identical to the previous inline construction (L66-72):
 *   [ffmpegPath, "-y", "-i", videoFile.absolutePath, "-vf",
 *    "subtitles=<subtitleFile.absolutePath>:force_style='<style>'",
 *    "-c:a", "copy", outputFile.absolutePath]
 *
 * `-c:a copy` preserves the audio stream unchanged.
 */
object SubtitleBurnInCommand {

    /**
     * Builds the full FFmpeg argv for subtitle burn-in.
     *
     * @param videoFile    the input WebM video
     * @param subtitleFile the SRT or VTT subtitle file (passed directly to
     *        the `subtitles=` filter — FFmpeg auto-detects the format by
     *        extension)
     * @param outputFile   the output WebM video with hard-burned subtitles
     * @param style        the burn-in style (font size/colour/position)
     * @param ffmpegPath   the ffmpeg executable path
     * @return the argv list (first element is the ffmpeg path)
     */
    fun buildArgv(
        videoFile: File,
        subtitleFile: File,
        outputFile: File,
        style: SubtitleBurnInStyle,
        ffmpegPath: String
    ): List<String> {
        val filter = "subtitles=${subtitleFile.absolutePath}:force_style='${style.toForceStyle()}'"
        return listOf(
            ffmpegPath, "-y",
            "-i", videoFile.absolutePath,
            "-vf", filter,
            "-c:a", "copy",
            outputFile.absolutePath
        )
    }
}