package capsule.pipeline

/**
 * Port — builds the LLM prompts for the capsule content pipeline nodes.
 *
 * Implementations assemble the system + user messages from the running
 * [CapsuleState]. Pure — no LLM, no I/O. Kept as a port so
 * [CapsulePipelineGraph] can be unit-tested with a deterministic stub,
 * without depending on Gradle or the production prompt engineering.
 */
interface CapsulePromptBuilder {

    /**
     * Prompt for the `propose-context` node — asks the LLM to propose a
     * content plan JSON blob from the deck name, language, source adoc and
     * augmented context.
     */
    fun buildProposePrompt(state: CapsuleState): String

    /**
     * Prompt for the `generate-speaker-notes` node — asks the LLM to generate
     * the enriched speaker notes AsciiDoc from the validated content plan.
     */
    fun buildGeneratePrompt(state: CapsuleState): String
}
