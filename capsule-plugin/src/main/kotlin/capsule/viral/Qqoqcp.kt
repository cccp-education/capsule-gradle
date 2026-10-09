package capsule.viral

/**
 * QQOQCP context of a viral capsule (CAP-CONTEXT US-1).
 *
 * The six decisive questions that *situate* the capsule (macro context, not a
 * questionnaire): who it addresses, what it shows, where/when it takes place,
 * how it is shown and why. Derived from the augmented N1 context (RAG/Graphify/
 * Docs/EAGER) and their provenance — never invented (CAP-VIRAL US-4/US-9).
 *
 * The short form (15-60 s) does not support overload (`CAPSULE_PEDAGOGIQUE.adoc`
 * § 4.1): the answers are concise.
 *
 * Property names map the French labels to non-keyword identifiers:
 * [audience]=Qui (public), [subject]=Quoi (sujet), [context]=Où (contexte),
 * [moment]=Quand (moment), [modality]=Comment (modalité), [purpose]=Pourquoi (but).
 *
 * Invariants (fail-fast): every answer must not be blank.
 *
 * @property audience  who the capsule addresses (Qui / public).
 * @property subject   the subject / behaviour shown (Quoi / sujet).
 * @property context   where it takes place (Où / contexte).
 * @property moment    at which point of the journey (Quand / moment).
 * @property modality  how it is shown (Comment / modalité).
 * @property purpose   the benefit / objective (Pourquoi / but).
 */
data class Qqoqcp(
    val audience: String,
    val subject: String,
    val context: String,
    val moment: String,
    val modality: String,
    val purpose: String,
) {
    init {
        require(audience.isNotBlank()) { "Qqoqcp.audience (Qui) must not be blank" }
        require(subject.isNotBlank()) { "Qqoqcp.subject (Quoi) must not be blank" }
        require(context.isNotBlank()) { "Qqoqcp.context (Où) must not be blank" }
        require(moment.isNotBlank()) { "Qqoqcp.moment (Quand) must not be blank" }
        require(modality.isNotBlank()) { "Qqoqcp.modality (Comment) must not be blank" }
        require(purpose.isNotBlank()) { "Qqoqcp.purpose (Pourquoi) must not be blank" }
    }
}
