package capsule.pipeline

/**
 * Port — the LLM provider consumed by [CapsulePipelineGraph].
 *
 * Capsule's LLM calls go through the codebase `LlmBuildService` bridge
 * (`capsule.ai.CapsuleLlmService`, CAP-ARCH-1) wrapping codebase's
 * `LlmProvider` (koog abstraction, suspend). [CapsulePipelineGraph] uses this
 * synchronous port so it stays Gradle-free, coroutine-free, and unit-testable
 * with a plain fake — the adapter mapping `suspend` → blocking lives outside
 * the domain (`capsule.pipeline` stays pure).
 *
 * Two operations match the two LLM-calling nodes of the pipeline:
 *  - [propose]  → the `propose-context` node (returns a content plan JSON blob).
 *  - [generate] → the `generate-speaker-notes` node (returns enriched AsciiDoc).
 */
interface CapsuleLlm {

    /** Calls the LLM with the propose-context prompt; returns a content plan JSON blob. */
    fun propose(prompt: String): String

    /** Calls the LLM with the generate-speaker-notes prompt; returns enriched speaker notes AsciiDoc. */
    fun generate(prompt: String): String
}
