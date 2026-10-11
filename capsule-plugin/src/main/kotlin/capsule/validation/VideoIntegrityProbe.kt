package capsule.validation

import capsule.ProcessRunner
import java.io.File

/**
 * I/O half of the video-integrity check (PRT-04).
 *
 * Runs `ffmpeg -v error -i <file> -f null -` to force a full decode, then hands
 * the raw outcome to [VideoIntegrityChecker]. The `null` muxer discards the
 * decoded frames (nothing is written); `-v error` keeps the diagnostic channel
 * empty unless something is genuinely wrong — so both the exit code and the
 * absence of output are meaningful signals.
 *
 * Distinct from `MediaProbeUtil.probeDuration`, which only reads metadata and
 * therefore cannot see a container whose stream is broken.
 */
object VideoIntegrityProbe {

    /**
     * The ffmpeg argv that decodes [file] without producing an output file.
     * Exposed for tests — the decoder command is part of the contract.
     */
    internal fun decodeArgs(file: File, ffmpegPath: String): List<String> =
        listOf(ffmpegPath, "-v", "error", "-i", file.absolutePath, "-f", "null", "-")

    /**
     * Decodes [file] and returns its integrity verdict via [VideoIntegrityChecker].
     *
     * A missing or empty file is [IntegrityResult.Corrupt] **without** spawning
     * ffmpeg. An unlaunchable decoder is also [IntegrityResult.Corrupt] — the
     * caller must never read "cannot check" as "clean".
     *
     * @param file           the media file (WebM/MP4) to decode.
     * @param ffmpegPath     the ffmpeg executable path.
     * @param timeoutMinutes guard bound; decoding a long master is fast but not
     *                       instantaneous, so it defaults to the process-wide
     *                       default rather than the short probe bound.
     */
    fun probe(
        file: File,
        ffmpegPath: String = "ffmpeg",
        timeoutMinutes: Long = ProcessRunner.DEFAULT_TIMEOUT_MINUTES,
    ): IntegrityResult {
        if (!file.exists() || file.length() == 0L) {
            return IntegrityResult.Corrupt("file missing or empty: ${file.name}")
        }
        return try {
            val result = ProcessRunner.run(decodeArgs(file, ffmpegPath), timeoutMinutes = timeoutMinutes)
            VideoIntegrityChecker.verdict(result.exitCode, result.output)
        } catch (e: Exception) {
            IntegrityResult.Corrupt("decoder could not be run: ${e.message?.take(200)}")
        }
    }
}
