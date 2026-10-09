package capsule.viral

/**
 * Validation verdict for a viral storyboard (CAP-VIRAL US-13).
 *
 * Sealed result (pattern `capsule.pipeline.ContentPlanValidationResult` /
 * `slider.pipeline.ValidationResult`): [Valid] or [Invalid] with the list of
 * human-readable reasons. The editorial gate only lets a [Valid] storyboard
 * proceed to rendering.
 */
sealed class StoryboardValidationResult {

    /** The storyboard satisfies every editorial rule. */
    object Valid : StoryboardValidationResult()

    /**
     * The storyboard violates at least one rule.
     *
     * @property reasons non-empty list of human-readable violations.
     */
    data class Invalid(val reasons: List<String>) : StoryboardValidationResult() {
        init {
            require(reasons.isNotEmpty()) { "StoryboardValidationResult.Invalid must carry at least one reason" }
        }
    }
}

/**
 * Pure validator of a viral storyboard — the editorial gate (CAP-VIRAL US-13).
 *
 * No video is rendered before a validated storyboard (mirror of rule A9
 * "corpus-clean before generate"). The rules enforce that the storyboard is a
 * *complete, reviewable* narrative:
 *  1. a `hook` beat is present (the 3-second hook);
 *  2. a `cta` beat is present (the closing call to action);
 *  3. the summed beat duration is within ±20 % of the target duration;
 *  4. no beat exceeds 10 s (short-form attention).
 *
 * Pure — no Gradle, no I/O.
 */
object ViralStoryboardValidator {

    /** A single beat may not exceed this many seconds. */
    const val MAX_BEAT_SECONDS: Double = 10.0

    /** Allowed relative deviation of the summed beats vs the target. */
    const val DURATION_TOLERANCE_RATIO: Double = 0.20

    /**
     * Validates [storyboard].
     *
     * @param storyboard the storyboard to validate.
     * @return [StoryboardValidationResult.Valid] or
     *         [StoryboardValidationResult.Invalid] with the violations.
     */
    fun validate(storyboard: ViralStoryboard): StoryboardValidationResult {
        val reasons = mutableListOf<String>()

        if (storyboard.hookBeat == null) {
            reasons += "storyboard has no 'hook' beat"
        }
        if (storyboard.ctaBeat == null) {
            reasons += "storyboard has no 'cta' beat"
        }

        val total = storyboard.totalBeatSeconds
        val target = storyboard.targetDurationSeconds.toDouble()
        val deviation = kotlin.math.abs(total - target) / target
        if (deviation > DURATION_TOLERANCE_RATIO) {
            reasons += "summed beat duration ${total}s deviates more than ${(DURATION_TOLERANCE_RATIO * 100).toInt()}% from target ${storyboard.targetDurationSeconds}s"
        }

        storyboard.beats.forEachIndexed { index, beat ->
            if (beat.durationSeconds > MAX_BEAT_SECONDS) {
                reasons += "beat ${index + 1} ('${beat.role}') lasts ${beat.durationSeconds}s (> ${MAX_BEAT_SECONDS}s)"
            }
        }

        return if (reasons.isEmpty()) StoryboardValidationResult.Valid else StoryboardValidationResult.Invalid(reasons)
    }
}
