package capsule

import capsule.validation.FfprobeDurationProbe
import java.io.File

/**
 * Shared utility for probing media file durations via ffprobe.
 *
 * Extracted to eliminate duplication across CapsuleVideoTask, ManimEngineImpl,
 * and ManimVideoMixerImpl.
 *
 * VK-4 US-4a — the duration probe now delegates to the shared N1 contract
 * ([video.kernel.VideoDurationProbe]) via [FfprobeDurationProbe], which reuses
 * the kernel's pure [video.kernel.FfprobeDurationParser]. This makes capsule and
 * hyperframes parse ffprobe output identically instead of each carrying its own.
 */
object MediaProbeUtil {

    /**
     * Probes the duration of a media file with ffprobe (default binary).
     */
    fun probeDuration(file: File): Double = probeDuration(file, "ffprobe")

    /**
     * Probes the duration of a media file with the ffprobe binary at [ffprobePath].
     *
     * @return duration in seconds, or 0.0 when ffprobe is missing, fails, or
     *         prints something that is not a number.
     */
    fun probeDuration(file: File, ffprobePath: String): Double =
        FfprobeDurationProbe(ffprobePath).probeSeconds(file)

    /**
     * Returns true if ffprobe is available in the system PATH.
     */
    fun isFfprobeAvailable(): Boolean = isFfprobeAvailable("ffprobe")

    /**
     * Returns true if the ffprobe binary at [ffprobePath] is available and executable.
     */
    fun isFfprobeAvailable(ffprobePath: String): Boolean = ProcessRunner.probe(ffprobePath, "-version")
}

