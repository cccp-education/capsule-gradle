package capsule.transcript

import capsule.feed.SlideSegment

/**
 * Immutable plan aggregating the inputs needed to build a transcript AsciiDoc
 * article from a capsule (CAP-TRANSCRIPT US-2).
 *
 * Reuses [capsule.feed.SlideSegment] as the segment model — no new model is
 * introduced. The plan is consumed by [TranscriptBuilder] (TEMPLATE strategy)
 * and [TranscriptLlmEnhancer] (LLM strategy, US-3).
 *
 * Invariants (fail-fast):
 * - [deckName] must not be blank.
 * - [language] must not be blank (ISO 639-1 code, e.g. "fr").
 * - [outputPath] must not be blank (destination `.adoc` file path).
 *
 * An empty [segments] list is allowed — a deck with zero narrated slides still
 * produces an intro+outro article (the builder never throws on empty input).
 *
 * @property deckName   deck name (without extension), must not be blank.
 * @property segments    ordered slide segments (may be empty).
 * @property language    target language code (ISO 639-1), must not be blank.
 * @property outputPath  destination `.adoc` file path, must not be blank.
 */
data class TranscriptPlan(
    val deckName: String,
    val segments: List<SlideSegment>,
    val language: String,
    val outputPath: String,
) {
    init {
        require(deckName.isNotBlank()) { "TranscriptPlan.deckName must not be blank" }
        require(language.isNotBlank()) { "TranscriptPlan.language must not be blank" }
        require(outputPath.isNotBlank()) { "TranscriptPlan.outputPath must not be blank" }
    }
}