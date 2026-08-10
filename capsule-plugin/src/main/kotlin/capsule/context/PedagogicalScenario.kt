package capsule.context

/**
 * Immutable pedagogical payload extracted from a training scenario companion
 * AsciiDoc (CAP-SPD-1).
 *
 * The K-2 `metadata.json` envelope carries only provenance (source, type,
 * sessions, generatedAt, model, version, dependencies) — the pedagogical
 * fields live in the AsciiDoc. This data class is the capsule-local consumer
 * model: capsule is format-agnostic (validates `type == "SPD"` as a content
 * type, never the producer borough), and owns the pedagogical scenario model
 * since there is no N0 contract for it.
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
data class PedagogicalScenario(
    val objectives: List<String>,
    val duration: String,
    val prerequisites: List<String>,
    val modalities: String,
    val sessionTitle: String,
    val module: String,
) {
    init {
        require(sessionTitle.isNotBlank()) {
            "PedagogicalScenario sessionTitle must be non-blank"
        }
    }

    /** `true` when the scenario carries no usable pedagogical signal. */
    val isEmpty: Boolean
        get() = objectives.isEmpty() && duration.isBlank() && prerequisites.isEmpty() && modalities.isBlank()
}