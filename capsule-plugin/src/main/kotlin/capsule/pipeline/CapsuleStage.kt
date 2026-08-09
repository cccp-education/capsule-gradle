package capsule.pipeline

/**
 * Stages of the capsule content generation pipeline — sequential progression
 * from initialization to the final enriched speaker notes (or failure).
 *
 * Pure enum — no Gradle, no LLM, no I/O. Drives the conditional edges of
 * [CapsulePipelineGraph] and the invariants of [CapsuleState].
 */
enum class CapsuleStage {
    /** Fresh state — deck, language, source adoc, augmented context set; no LLM call yet. */
    INITIALIZED,

    /** LLM has proposed a content plan JSON via the `propose-context` node. */
    CONTEXT_PROPOSED,

    /** The proposed content plan JSON has passed [ContentPlanValidator]. */
    CONTEXT_VALIDATED,

    /** LLM has generated the enriched speaker notes + TTS script via `generate-speaker-notes`. */
    CONTENT_GENERATED,

    /** Pipeline failed at any node — see [CapsuleState.error] for the reason. */
    FAILED,
}
