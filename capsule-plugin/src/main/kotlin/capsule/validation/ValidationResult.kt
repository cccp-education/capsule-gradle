package capsule.validation

import java.util.Locale

/**
 * Result of validating the capsule video duration against the sum of
 * audio (TTS) durations (CAP-CR3-1 US-1).
 *
 * Sealed type modeled on `slider.pipeline.ValidationResult` (SLD-8.3
 * `DeckContextValidator`). Two variants:
 *
 * - [Valid] — the delta between video and audio sum is within tolerance.
 * - [Invalid] — the delta exceeds tolerance; the capsule video is
 *   likely truncated or the audio is misaligned.
 *
 * Both variants carry the computed [delta] (absolute difference in
 * seconds) so callers can log it regardless of the outcome.
 */
sealed class ValidationResult {

    /**
     * The absolute difference between the video duration and the audio
     * sum, in seconds.
     */
    abstract val delta: Double

    /**
     * Validation passed — the video duration matches the audio sum
     * within the configured tolerance.
     */
    data class Valid(override val delta: Double) : ValidationResult()

    /**
     * Validation failed — the video duration diverges from the audio
     * sum by more than the configured tolerance.
     *
     * @param videoDuration  the probed video duration in seconds.
     * @param sumAudio       the sum of the probed audio durations in seconds.
     * @param toleranceSecs  the tolerance threshold in seconds.
     */
    data class Invalid(
        override val delta: Double,
        val videoDuration: Double,
        val sumAudio: Double,
        val toleranceSecs: Double
    ) : ValidationResult() {

        /**
         * Returns an actionable, human-readable error message explaining
         * the mismatch. Used as the `GradleException` message by the
         * `validateCapsuleVideoDuration` task.
         */
        fun message(): String {
            return "Duration mismatch: video=${formatDuration(videoDuration)}s, " +
                "audio=${formatDuration(sumAudio)}s, " +
                "delta=${formatDuration(delta)}s, " +
                "tolerance=${formatDuration(toleranceSecs)}s"
        }

        private fun formatDuration(value: Double): String =
            String.format(Locale.US, "%.2f", value)
    }
}