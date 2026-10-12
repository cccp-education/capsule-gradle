package capsule

import java.io.File

/**
 * Pure FFmpeg argv builder for the WebM → MP4/H.264 transcode (CAP-MP4 US-2,
 * PRT-09 residual).
 *
 * Mirrors the other pure command objects ([AudioPostCommand],
 * [SubtitleBurnInCommand], [capsule.podcast.PodcastConcatCommand]): the argument
 * list is a pure function of its inputs, unit-testable without invoking ffmpeg.
 *
 * Colorimetry: when [bt709] is enabled the encoder tags the output as BT.709
 * (`-color_primaries bt709 -color_trc bt709 -colorspace bt709`) — the broadcast
 * convention a video course master should declare. When disabled the argv is
 * byte-identical to the historical one (backward compatibility).
 */
object Mp4ConversionCommand {

    /** Builds the ffmpeg argument list transcoding [input] (WebM) to [output] (MP4/H.264). */
    fun buildArgv(
        ffmpegPath: String,
        input: File,
        output: File,
        bt709: Boolean,
    ): List<String> {
        val cmd = mutableListOf(
            ffmpegPath, "-y",
            "-i", input.absolutePath,
            "-c:v", "libx264",
            "-c:a", "aac",
            "-movflags", "+faststart",
        )
        if (bt709) {
            cmd += listOf(
                "-color_primaries", "bt709",
                "-color_trc", "bt709",
                "-colorspace", "bt709",
            )
        }
        cmd += output.absolutePath
        return cmd
    }
}
