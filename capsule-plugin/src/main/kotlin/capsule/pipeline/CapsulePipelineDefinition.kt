package capsule.pipeline

import codebase.koog.pipeline.PipelineDefinition

/**
 * Domain port implementation driving the shared N1 pipeline skeleton
 * `codebase.koog.pipeline.PipelineGraph` for the capsule content generation
 * pipeline (EPIC KOOG-PIPELINE-DEDUP, BS-2 adoption).
 *
 * Only the capsule-specific knowledge lives here — the LLM calls, the prompt
 * builder, the [ContentPlanValidator] delegation and the failure labels. The
 * orchestration (koog topology, sequential fail-fast [execute], mermaid
 * diagram) is the shared `PipelineGraph`.
 *
 * Nodes:
 *  1. [propose] — calls [CapsuleLlm.propose] with the prompt built from the
 *     [CapsuleState]; stores the returned content plan JSON and advances to
 *     [CapsuleStage.CONTEXT_PROPOSED].
 *  2. [validate] — pure — delegates to [ContentPlanValidator]. Valid advances
 *     to [CapsuleStage.CONTEXT_VALIDATED] with `contextValid = true`; Invalid
 *     advances to [CapsuleStage.FAILED] with the validator error.
 *  3. [generate] — calls [CapsuleLlm.generate] with the validated state, stores
 *     the enriched AsciiDoc, derives the TTS script and advances to
 *     [CapsuleStage.CONTENT_GENERATED].
 *
 * @param promptBuilder builds the LLM prompts from the running [CapsuleState].
 * @param llm           the LLM provider (production: adapter on codebase's
 *                      `LlmBuildService`; tests: a fake).
 */
class CapsulePipelineDefinition(
    private val promptBuilder: CapsulePromptBuilder,
    private val llm: CapsuleLlm,
) : PipelineDefinition<CapsuleState> {

    override val name: String = NAME

    override fun propose(state: CapsuleState): CapsuleState {
        val prompt = promptBuilder.buildProposePrompt(state)
        val contentPlanJson = llm.propose(prompt)
        require(contentPlanJson.isNotBlank()) { "LLM returned a blank content plan JSON" }
        return state.copy(
            contentPlanJson = contentPlanJson,
            stage = CapsuleStage.CONTEXT_PROPOSED,
        )
    }

    override fun validate(state: CapsuleState): CapsuleState =
        when (val result = ContentPlanValidator.validate(state.contentPlanJson)) {
            is ContentPlanValidationResult.Valid -> state.copy(
                contextValid = true,
                stage = CapsuleStage.CONTEXT_VALIDATED,
            )
            is ContentPlanValidationResult.Invalid -> state.copy(
                contextValid = false,
                validationError = result.error,
                error = result.error,
                stage = CapsuleStage.FAILED,
            )
        }

    override fun generate(state: CapsuleState): CapsuleState {
        val prompt = promptBuilder.buildGeneratePrompt(state)
        val speakerNotesAdoc = llm.generate(prompt)
        require(speakerNotesAdoc.isNotBlank()) { "LLM returned a blank speaker notes AsciiDoc" }
        return state.copy(
            speakerNotesAdoc = speakerNotesAdoc,
            ttsScript = TtsScriptDeriver.derive(speakerNotesAdoc, state.deckName),
            stage = CapsuleStage.CONTENT_GENERATED,
        )
    }

    override fun isValid(state: CapsuleState): Boolean = state.contextValid

    override fun isFailed(state: CapsuleState): Boolean = state.stage == CapsuleStage.FAILED

    override fun onProposeFailure(state: CapsuleState, cause: Throwable): CapsuleState =
        state.copy(
            stage = CapsuleStage.FAILED,
            error = "ProposeContextFailed: ${cause.message}",
        )

    override fun onGenerateFailure(state: CapsuleState, cause: Throwable): CapsuleState =
        state.copy(
            stage = CapsuleStage.FAILED,
            error = "GenerateSpeakerNotesFailed: ${cause.message}",
        )

    companion object {
        /** Strategy name exposed by the built koog graph. */
        const val NAME: String = "capsule-pipeline"
    }
}
