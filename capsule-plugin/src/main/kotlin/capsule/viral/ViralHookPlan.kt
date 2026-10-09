package capsule.viral

import capsule.feed.SlideSegment

/**
 * Immutable plan aggregating the inputs needed to generate a viral campaign's
 * hook, message and call-to-action (CAP-VIRAL US-4).
 *
 * The generation is **anchored on the N1 augmented context** produced by
 * codebase (`contracts.context.CompositeContext` rendered by
 * `capsule.context.CapsuleContextBuilder` — RAG/Graphify/Docs/EAGER channels +
 * provenance). The plan carries the already-rendered [augmentedContext] block so
 * the domain stays pure (no Gradle, no network): the caller assembles the
 * context, this plan carries it, and the prompt embeds it.
 *
 * Invariants (fail-fast):
 * - [deckName] must not be blank.
 * - [language] must not be blank (ISO 639-1 code, e.g. "fr").
 * - [targetDurationSeconds] must be within the short-form range [15, 60].
 * - [outputPath] must not be blank.
 *
 * [augmentedContext] may be blank (no corpus available) — generation then
 * falls back to the deck's own content, degraded but functional.
 *
 * @property deckName              deck name (without extension), not blank.
 * @property segments              ordered slide segments (source of the message).
 * @property language              target language code, not blank.
 * @property platform              target platform preset.
 * @property targetDurationSeconds short-form target duration, in [15, 60].
 * @property augmentedContext      prompt-ready N1 context block (may be blank).
 * @property outputPath            destination path (not blank).
 */
data class ViralHookPlan(
    val deckName: String,
    val segments: List<SlideSegment>,
    val language: String,
    val platform: ViralPlatform,
    val targetDurationSeconds: Int,
    val augmentedContext: String,
    val outputPath: String,
) {
    init {
        require(deckName.isNotBlank()) { "ViralHookPlan.deckName must not be blank" }
        require(language.isNotBlank()) { "ViralHookPlan.language must not be blank" }
        require(outputPath.isNotBlank()) { "ViralHookPlan.outputPath must not be blank" }
        require(targetDurationSeconds in ViralConfig.MIN_DURATION_SECONDS..ViralConfig.MAX_DURATION_SECONDS) {
            "ViralHookPlan.targetDurationSeconds must be in [${ViralConfig.MIN_DURATION_SECONDS}, ${ViralConfig.MAX_DURATION_SECONDS}], got $targetDurationSeconds"
        }
    }

    /** `true` when an anchored N1 context is available. */
    val isAnchored: Boolean get() = augmentedContext.isNotBlank()
}
