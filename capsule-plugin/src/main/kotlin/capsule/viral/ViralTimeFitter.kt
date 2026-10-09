package capsule.viral

/**
 * Pure time-fit of the viral montage (CAP-SHORT US-2).
 *
 * The storyboard's beats sum to a value within ±20 % of the target (editorial
 * gate, [ViralStoryboardValidator]), but the *montage* must honour the target
 * **exactly** so a short-form capsule lands in the platform's [15, 60] window.
 * This object **trims or pads** the beats proportionally — the relative pacing
 * is preserved, only the cadence is fitted — and [isFitted] gates the produced
 * video's probed duration against the target.
 *
 * Pure — no Gradle, no I/O, no FFmpeg. Deterministic.
 */
object ViralTimeFitter {

    /**
     * Fits [beatDurations] so their sum equals [targetSeconds] exactly.
     *
     * Proportionally scales every beat by `target / Σbeats`; the residual of the
     * double arithmetic is absorbed by the last beat so the sum is exactly the
     * (clamped) target.
     *
     * @param beatDurations the storyboard beat durations (seconds), all > 0.
     * @param targetSeconds  the target short-form duration, clamped to [15, 60].
     * @return the fitted durations, same size and order as the input.
     */
    fun fit(beatDurations: List<Double>, targetSeconds: Int): List<Double> {
        require(beatDurations.isNotEmpty()) { "ViralTimeFitter.fit requires at least one beat" }
        require(beatDurations.all { it > 0.0 }) {
            "ViralTimeFitter.fit requires positive beat durations, got $beatDurations"
        }
        val target = targetSeconds
            .coerceIn(ViralConfig.MIN_DURATION_SECONDS, ViralConfig.MAX_DURATION_SECONDS)
            .toDouble()
        val total = beatDurations.sum()
        val scaled = beatDurations.map { it * target / total }
        val residual = target - scaled.sum()
        return scaled.dropLast(1) + (scaled.last() + residual)
    }

    /**
     * `true` when the produced [actualSeconds] lands within [toleranceSecs] of the
     * target short-form duration — the montage gate.
     */
    fun isFitted(actualSeconds: Double, targetSeconds: Int, toleranceSecs: Double): Boolean =
        kotlin.math.abs(actualSeconds - targetSeconds) <= toleranceSecs
}
