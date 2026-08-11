package capsule.validation

/**
 * Pure domain service that validates the capsule video duration against
 * the sum of audio (TTS) durations (CAP-CR3-1 US-1).
 *
 * The capsule pipeline produces a WebM (or MP4) whose duration should
 * match the sum of the per-slide TTS MP3 durations. A divergence
 * indicates a broken capture (truncated video) or a misaligned audio
 * mix (silent gaps). This validator computes the delta and returns a
 * [ValidationResult] — [ValidationResult.Valid] when within tolerance,
 * [ValidationResult.Invalid] otherwise.
 *
 * Pure — no Gradle, no I/O, no ffprobe. The caller (the
 * `validateCapsuleVideoDuration` task) probes the durations via
 * `MediaProbeUtil` and passes the raw numbers. This keeps the domain
 * logic fully testable without subprocess execution.
 *
 * Pattern aligned on `slider.pipeline.DeckContextValidator` (SLD-8.3 —
 * object pur, sealed result).
 */
object VideoDurationValidator {

    /**
     * Validates that the [videoDurationSecs] matches the sum of
     * [audioDurationsSecs] within [toleranceSecs].
     *
     * The delta is computed as `abs(videoDuration - sumAudio)`.
     * When the audio list is empty, the sum is 0.0 — the validator
     * returns [ValidationResult.Invalid] unless the video duration
     * itself is also 0.0 and the tolerance is >= 0.0 (degenerate but
     * consistent case).
     *
     * @param videoDurationSecs   the probed video duration in seconds
     *        (0.0 when the file is missing or ffprobe is unavailable).
     * @param audioDurationsSecs  the per-slide probed audio durations
     *        in seconds (one entry per slide TTS MP3).
     * @param toleranceSecs       the tolerance threshold in seconds.
     *        A delta <= tolerance is considered valid.
     * @return [ValidationResult.Valid] when within tolerance,
     *         [ValidationResult.Invalid] otherwise.
     */
    fun validate(
        videoDurationSecs: Double,
        audioDurationsSecs: List<Double>,
        toleranceSecs: Double
    ): ValidationResult {
        val sumAudio = audioDurationsSecs.sum()
        val delta = kotlin.math.abs(videoDurationSecs - sumAudio)
        return if (delta <= toleranceSecs) {
            ValidationResult.Valid(delta)
        } else {
            ValidationResult.Invalid(delta, videoDurationSecs, sumAudio, toleranceSecs)
        }
    }
}