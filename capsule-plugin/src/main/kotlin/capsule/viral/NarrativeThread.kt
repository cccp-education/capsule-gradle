package capsule.viral

/**
 * Narrative thread of a viral capsule (CAP-CONTEXT US-1).
 *
 * The coherence *contract* between beats: the [thesis] (angle, one sentence),
 * the [recurringMotif] (visual/lexical element recalled from one beat to the
 * next), the [arc] (hook → development → resolution → CTA), the [register] and
 * the [voice]. It is what makes the sequences a narrative rather than a list of
 * shots; the editorial gate (CAP-CONTEXT US-3) can then verify continuity, not
 * only the presence of hook/CTA.
 *
 * Invariants (fail-fast): [thesis], [recurringMotif] and [arc] must not be blank.
 *
 * @property thesis         the point of view, in one sentence.
 * @property recurringMotif the element recalled from one sequence to the next.
 * @property arc            the narrative arc (hook → … → CTA).
 * @property register       the register (pédagogique | promotionnel | témoignage).
 * @property voice          the voice / tone (optional).
 */
data class NarrativeThread(
    val thesis: String,
    val recurringMotif: String,
    val arc: String,
    val register: String = "",
    val voice: String = "",
) {
    init {
        require(thesis.isNotBlank()) { "NarrativeThread.thesis must not be blank" }
        require(recurringMotif.isNotBlank()) { "NarrativeThread.recurringMotif must not be blank" }
        require(arc.isNotBlank()) { "NarrativeThread.arc must not be blank" }
    }
}
