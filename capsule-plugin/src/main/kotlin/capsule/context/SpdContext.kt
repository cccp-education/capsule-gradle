package capsule.context

/**
 * Immutable pedagogical payload extracted from a SPD (Scénario Pédagogique
 * Détaillé) companion AsciiDoc (CAP-SPD-1).
 *
 * The K-2 `metadata.json` envelope carries only provenance (source, type,
 * sessions, generatedAt, model, version, dependencies) — the pedagogical
 * fields live in the AsciiDoc. This data class is the capsule-local consumer
 * model: there is no N0 contract for SPD, so capsule owns it.
 *
 * @property objectives    bullet objectives extracted from `== Objectifs`
 *                         (empty list when the section is absent).
 * @property duration       session duration (empty string when unspecified).
 * @property prerequisites  bullet prerequisites from `== Prérequis`
 *                         (empty list when the section is absent).
 * @property modalities     evaluation modalities from `== Modalités d'évaluation`
 *                         (empty string when the section is absent).
 * @property sessionTitle   first `= Title` heading of the AsciiDoc, or the
 *                         filename fallback. Non-blank by invariant.
 * @property module         `:module:` document attribute, or empty string.
 */
data class SpdContext(
    val objectives: List<String>,
    val duration: String,
    val prerequisites: List<String>,
    val modalities: String,
    val sessionTitle: String,
    val module: String,
) {
    init {
        require(sessionTitle.isNotBlank()) {
            "SpdContext sessionTitle must be non-blank"
        }
    }

    /** `true` when the SPD carries no usable pedagogical signal. */
    val isEmpty: Boolean
        get() = objectives.isEmpty() && duration.isBlank() && prerequisites.isEmpty() && modalities.isBlank()
}