package capsule.kernel

import contracts.i18n.SupportedLanguage

/**
 * Immutable plan of video productions across one or more target languages
 * (CAP-ENGINE-DECOUPLING US-1).
 *
 * Replaces the former `capsule.multilang.CapsuleVideoPlan` with the same shape
 * and invariants, now expressed in the neutral kernel.
 *
 * Invariant: at least one [VideoProductionEntry] (an empty plan has no work to
 * do and signals a misconfiguration upstream — fail-fast rather than silently
 * producing nothing).
 *
 * @property entries non-empty list of per-language production units.
 */
data class VideoProductionPlan(
    val entries: List<VideoProductionEntry>,
) {
    init {
        require(entries.isNotEmpty()) {
            "VideoProductionPlan requires at least one entry"
        }
    }

    /** Number of per-language video productions in this plan. */
    fun size(): Int = entries.size

    /** `true` only when the plan holds zero entries (never reachable — the
     *  invariant throws before construction completes). */
    fun isEmpty(): Boolean = entries.isEmpty()

    /** Target languages covered by this plan, in entry order. */
    fun languages(): List<SupportedLanguage> = entries.map { it.language }
}
