package capsule.validation

import capsule.ProcessRunner
import video.kernel.FfprobeDurationParser
import video.kernel.VideoDurationProbe
import java.io.File

/**
 * `ffprobe`-backed [VideoDurationProbe] (EPIC VK-4 US-4a).
 *
 * N2 implementation of the shared N1 port: capsule consumes the kernel's
 * [VideoDurationProbe] contract and its pure [FfprobeDurationParser], and keeps
 * only the external-process coupling (`capsule.ProcessRunner`, which guards
 * against the pipe-deadlock described in its KDoc) — the kernel itself forbids
 * `ProcessBuilder` (`video.ci.KernelBoundaryAudit`).
 *
 * Degrades to `0.0` when the file is missing/empty, when `ffprobe` fails, or
 * when the output is unparsable — a build must never fail because an optional
 * prober is unavailable.
 *
 * @param ffprobePath resolved `ffprobe` binary (default: `PATH` lookup).
 * @param runner      process seam (test-injectable); returns the captured
 *                    stdout, or an empty string on failure.
 */
class FfprobeDurationProbe(
    private val ffprobePath: String = "ffprobe",
    private val runner: (List<String>) -> String = { argv ->
        ProcessRunner.run(argv, timeoutMinutes = ProcessRunner.PROBE_TIMEOUT_MINUTES).output
    },
) : VideoDurationProbe {

    override fun probeSeconds(video: File): Double {
        if (!video.exists() || video.length() == 0L) return 0.0
        val argv = listOf(
            ffprobePath, "-v", "quiet",
            "-show_entries", "format=duration",
            "-of", "csv=p=0",
            video.absolutePath,
        )
        return try {
            FfprobeDurationParser.parse(runner(argv)) ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }
}
