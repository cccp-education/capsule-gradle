package capsule.context

/**
 * Immutable glossary term-definition pair (CAP-GLOSSARY-1).
 *
 * A glossary is an ordered list of referential terms (AFNOR/REAC vocabulary):
 * each entry binds a `term` to its `definition`. Both fields must be non-blank
 * (fail-fast invariant) — an empty term or definition is a malformed bullet
 * and must be rejected at the model boundary, not silently dropped downstream.
 *
 * The order of entries in a [GlossaryContext] is pedagogically meaningful (the
 * first term is the most important), which is why the glossary is a list and
 * not a map.
 *
 * @property term       the referential term (non-blank by invariant).
 * @property definition the term definition (non-blank by invariant).
 */
data class GlossaryEntry(
    val term: String,
    val definition: String,
) {
    init {
        require(term.isNotBlank()) { "GlossaryEntry term must be non-blank" }
        require(definition.isNotBlank()) { "GlossaryEntry definition must be non-blank" }
    }
}