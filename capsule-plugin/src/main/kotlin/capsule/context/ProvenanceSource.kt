package capsule.context

/**
 * Immutable descriptor of a single source that fed one [ProvenanceChannel]
 * of the capsule augmented context (CAP-PROVENANCE).
 *
 * Carries the human-readable [fileName] plus the measured [chars] and
 * estimated [tokens] so the `context-provenance.json` artefact can prove
 * where the context content came from (anti-hallucination audit). Values are
 * computed by the wiring layer — no `init` fail-fast (they are measurements,
 * not user input).
 *
 * @property fileName  source file name or injected-channel label (e.g.
 *                     `rag-injected`), as recorded by the collector.
 * @property chars     raw character count of the source content.
 * @property tokens    estimated token count (N0 [contracts.context.ContextChannel.estimateTokens]).
 */
data class ProvenanceSource(
    val fileName: String,
    val chars: Int = 0,
    val tokens: Int = 0,
)
