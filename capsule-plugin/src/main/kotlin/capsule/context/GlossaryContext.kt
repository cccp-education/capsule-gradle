package capsule.context

/**
 * Immutable ordered glossary extracted from a training referential AsciiDoc
 * (CAP-GLOSSARY-1).
 *
 * A glossary is an ordered list of [GlossaryEntry] — the order is
 * pedagogically meaningful (the first term is the most important), which is
 * why the glossary is a list and not a map. An empty [terms] list is
 * legitimate (glossary section absent or no valid bullets) — no fail-fast
 * invariant on the list size. The loader returns a blank string in that case,
 * and a [GlossaryContext] is only instantiated when at least one term is
 * extracted.
 *
 * @property terms the ordered glossary entries (may be empty).
 */
data class GlossaryContext(
    val terms: List<GlossaryEntry>,
)