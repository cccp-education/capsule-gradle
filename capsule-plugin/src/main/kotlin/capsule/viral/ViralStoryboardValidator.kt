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
 * CAP-CONTEXT US-3 adds the **context coherence** rules of the `CAPSULE_VIDEO`
 * Gate ("fil conducteur : arc complet + transition par séquence"). They apply
 * **only when the storyboard carries context** — a context-less storyboard keeps
 * the exact CAP-VIRAL verdict (backward compat):
 *  5. the three context axes are all-or-none (a partially contextualised
 *     storyboard is incoherent);
 *  6. when a [NarrativeThread] is carried, each beat after the first declares an
 *     entering transition (the narrative continuity contract).
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

        reasons += coherenceReasons(storyboard)

        return if (reasons.isEmpty()) StoryboardValidationResult.Valid else StoryboardValidationResult.Invalid(reasons)
    }

    /**
     * CAP-CONTEXT US-3 coherence reasons. Empty when the storyboard carries no
     * context (backward compat — the CAP-VIRAL verdict is preserved).
     */
    private fun coherenceReasons(storyboard: ViralStoryboard): List<String> {
        val reasons = mutableListOf<String>()

        val presentAxes = listOfNotNull(storyboard.qqoqcp, storyboard.visualIdentity, storyboard.narrativeThread)
        if (presentAxes.isNotEmpty() && presentAxes.size < CONTEXT_AXIS_COUNT) {
            reasons += "storyboard context is partial: QQOQCP, visual identity and narrative thread must be provided together"
        }

        if (storyboard.narrativeThread != null) {
            storyboard.beats.drop(1).forEachIndexed { index, beat ->
                if (beat.enteringTransition.isBlank()) {
                    reasons += "beat ${index + 2} ('${beat.role}') has no entering transition (narrative thread continuity)"
                }
            }
        }

        return reasons
    }

    /** Number of context axes of `CAPSULE_VIDEO.adoc` (QQOQCP, identity, thread). */
    private const val CONTEXT_AXIS_COUNT = 3
}
