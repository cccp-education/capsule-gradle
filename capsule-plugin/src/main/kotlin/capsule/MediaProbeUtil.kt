package capsule

import java.io.File

/**
 * Shared utility for probing media file durations via ffprobe.
 *
 * Extracted to eliminate duplication across CapsuleVideoTask, ManimEngineImpl,
 * and ManimVideoMixerImpl.
 */
object MediaProbeUtil {

    /**
     * Probes the duration of [file] in seconds using ffprobe from the system PATH.
     * Returns 0.0 if the file does not exist, ffprobe is unavailable, or parsing fails.
     */
    fun probeDuration(file: File): Double = probeDuration(file, "ffprobe")

    /**
     * Probes the duration of [file] in seconds using the specified [ffprobePath].
     * Returns 0.0 if the file does not exist, ffprobe is unavailable, or parsing fails.
     */
    fun probeDuration(file: File, ffprobePath: String): Double {
        return try {
            val proc = ProcessBuilder(
                ffprobePath, "-v", "quiet",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                file.absolutePath
            ).redirectErrorStream(true).start()
            val out = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            out.toDoubleOrNull() ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    /**
     * Returns true if ffprobe is available in the system PATH.
     */
    fun isFfprobeAvailable(): Boolean = isFfprobeAvailable("ffprobe")

    /**
     * Returns true if the ffprobe binary at [ffprobePath] is available and executable.
     */
    fun isFfprobeAvailable(ffprobePath: String): Boolean {
        return try {
            val proc = ProcessBuilder(ffprobePath, "-version")
                .redirectErrorStream(true)
                .start()
            proc.waitFor()
            proc.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }
}
