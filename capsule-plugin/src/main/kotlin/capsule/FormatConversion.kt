package capsule

import java.io.File

/**
 * Pure dispatch logic for post-capture format conversion (CAP-MP4 US-2).
 *
 * [convertIfNeeded] dispatches on [OutputFormat] after the burn-in step:
 * - [OutputFormat.WEBM] → no-op, returns the WebM file unchanged.
 * - [OutputFormat.MP4]  → transcode via the converter; on success removes the
 *   WebM intermediate and returns the MP4 file. On failure, keeps the WebM
 *   (degraded mode — backward compat).
 * - [OutputFormat.BOTH] → transcode and keep the WebM; returns the WebM
 *   (primary, unchanged contract).
 *
 * Economy of ink (AGENT.adoc): if the target MP4 already exists and
 * `probeDuration(mp4) > 0`, the transcode is skipped — the existing MP4
 * is the truth. The WebM intermediate is still removed when format == MP4
 * (the user asked for MP4 only).
 *
 * Pure object — no I/O except via the injected [VideoFormatConverter] and
 * `probeDuration` lambda. Fully testable.
 */
object FormatConversion {

    /**
     * @param finalVideo    the WebM produced by the capture+burn-in step
     * @param format        the resolved output format
     * @param converter     the resolved video format converter
     * @param probeDuration lambda returning the probed duration in seconds
     *        (0.0 if file missing/invalid) — injected so tests don't need ffprobe
     * @return the final video file (WebM for WEBM/BOTH, MP4 for MP4 success,
     *         WebM on MP4 failure)
     */
    fun convertIfNeeded(
        finalVideo: File,
        format: OutputFormat,
        converter: VideoFormatConverter,
        probeDuration: (File) -> Double
    ): File {
        if (format == OutputFormat.WEBM) return finalVideo

        val mp4File = File(finalVideo.parentFile, finalVideo.nameWithoutExtension + ".mp4")

        // Economy of ink: skip transcode if MP4 already exists and is valid.
        if (mp4File.exists() && probeDuration(mp4File) > 0.0) {
            if (format == OutputFormat.MP4) {
                finalVideo.delete()
                return mp4File
            }
            // BOTH: keep the WebM, reuse existing MP4.
            return finalVideo
        }

        val success = converter.convertToMp4(finalVideo, mp4File)
        if (!success) {
            // Degraded: keep WebM, no MP4 produced.
            return finalVideo
        }

        if (format == OutputFormat.MP4) {
            finalVideo.delete()
            return mp4File
        }
        // BOTH: keep both, return WebM (primary contract).
        return finalVideo
    }
}