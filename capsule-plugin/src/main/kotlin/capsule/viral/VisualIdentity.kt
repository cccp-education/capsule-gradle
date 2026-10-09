package capsule.viral

/**
 * Visual identity (design system) of a viral capsule (CAP-CONTEXT US-1).
 *
 * The locked dressing of a capsule: the [brand], the [format], the design
 * [tokens], the [typography], the [logo] and the composition [background]. The
 * identity *dresses* the message — it never replaces it: it is a parameter of
 * the rendering, never the entry of the storyboard (`CAPSULE_PEDAGOGIQUE.adoc`
 * § 4.1). The tokens come from the brand book (`BRAND.adoc`, talaria.school);
 * the storyboard *references* them without reinventing them.
 *
 * Invariants (fail-fast): [brand] and [format] must not be blank, [tokens] must
 * not be empty, and no token key may be blank.
 *
 * @property brand      the brand whose chart governs the capsule.
 * @property format     the output format (e.g. `9:16 (1080x1920)`).
 * @property tokens     design tokens (token name → value).
 * @property typography the typeface (optional).
 * @property logo       the logo reference (optional).
 * @property background the composition background (optional, never hardcoded).
 */
data class VisualIdentity(
    val brand: String,
    val format: String,
    val tokens: Map<String, String>,
    val typography: String = "",
    val logo: String = "",
    val background: String = "",
) {
    init {
        require(brand.isNotBlank()) { "VisualIdentity.brand must not be blank" }
        require(format.isNotBlank()) { "VisualIdentity.format must not be blank" }
        require(tokens.isNotEmpty()) { "VisualIdentity.tokens must not be empty" }
        require(tokens.keys.none { it.isBlank() }) { "VisualIdentity.tokens keys must not be blank" }
    }
}
