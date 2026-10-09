package capsule.viral

/**
 * Immutable result of the viral hook generation (CAP-VIRAL US-4).
 *
 * Carries the three textual elements of a short-form campaign:
 * - [hook]    — the opening 3-second hook (accroche).
 * - [message] — the point of view / message the campaign exposes.
 * - [cta]     — the closing call to action.
 *
 * Invariant: at least one field must be non-blank (an all-blank result is
 * meaningless and the generator degrades to the deck's first segment instead).
 *
 * @property hook    opening hook (may be blank if the LLM omitted it).
 * @property message the campaign message / point of view.
 * @property cta     closing call to action (may be blank).
 */
data class ViralHook(
    val hook: String,
    val message: String,
    val cta: String,
) {
    init {
        require(hook.isNotBlank() || message.isNotBlank() || cta.isNotBlank()) {
            "ViralHook must carry at least one non-blank field"
        }
    }

    /** `true` when the hook is present (non-blank). */
    val hasHook: Boolean get() = hook.isNotBlank()

    /** `true` when the call to action is present (non-blank). */
    val hasCta: Boolean get() = cta.isNotBlank()
}
