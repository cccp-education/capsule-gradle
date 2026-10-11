package capsule.viral

/**
 * Pure normalizer of a viral [ViralStoryboard] (storyboard authoring helper).
 *
 * The editorial gate ([ViralStoryboardValidator]) rejects a storyboard that
 * carries a [NarrativeThread] but omits a beat's entering transition (CAP-CONTEXT
 * US-3). Authoring a storyboard by hand thus requires a transition on every beat
 * after the opening — a repeated, error-prone chore.
 *
 * This object fills the *missing* transitions from a role-based default policy:
 * the opening `hook` transitions in, a `development` beat cuts, the closing `cta`
 * cross-fades. It is a **bootstrap/normalization** helper — the gate remains the
 * guard for hand-authored storyboards, and an explicit transition is always
 * preserved.
 *
 * Distinct from [ViralStoryboardBuilder] (render) and [ViralStoryboardParser]
 * (read): this object *transforms* a parsed storyboard, purely.
 */
object ViralStoryboardNormalizer {

    /** Role-based default entering transition (`hook`/`development`/`cta`). */
    val DEFAULT_TRANSITIONS: Map<String, String> = mapOf(
        "hook" to "— (ouverture)",
        "development" to "cut",
        "cta" to "fade",
    )

    /** Fallback transition for a role outside the default policy. */
    const val FALLBACK_TRANSITION: String = "cut"

    /** Resolves the default entering transition for [role] (case-insensitive). */
    fun defaultFor(role: String): String =
        DEFAULT_TRANSITIONS[role.lowercase().trim()] ?: FALLBACK_TRANSITION

    /**
     * Returns a copy of [storyboard] where every beat carries a non-blank
     * entering transition — an explicit transition is preserved, a missing one is
     * filled from [defaultFor]. Idempotent; touches no other field.
     *
     * @param storyboard the storyboard to normalize.
     * @return the normalized storyboard.
     */
    fun normalize(storyboard: ViralStoryboard): ViralStoryboard {
        val beats = storyboard.beats.map { beat ->
            if (beat.enteringTransition.isNotBlank()) beat
            else beat.copy(enteringTransition = defaultFor(beat.role))
        }
        return if (beats == storyboard.beats) storyboard else storyboard.copy(beats = beats)
    }
}
