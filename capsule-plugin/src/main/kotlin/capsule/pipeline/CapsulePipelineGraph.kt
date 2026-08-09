package capsule.pipeline

import ai.koog.agents.core.agent.asMermaidDiagram
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Koog-orchestrated capsule content generation pipeline — the heart of
 * EPIC CAP-ARCH-3.
 *
 * Architecture ("koog orchestrates, langchain4j executes"):
 *  - The [graph] is a koog [AIAgentGraphStrategy] declaring 3 nodes wired by
 *    conditional edges. It captures the *topology* of the pipeline and is
 *    queryable via [asMermaidDiagram].
 *  - The [execute] method runs the pipeline sequentially with per-node
 *    try/catch, mirroring `slider.pipeline.DeckPipelineGraph.execute` and
 *    [codebase.koog.KoogAugmentedContextGraph.execute]. This keeps failure
 *    modes explicit and the unit tests free from koog's async runtime.
 *
 * Nodes:
 *  1. [proposeContext] — calls [CapsuleLlm.propose] with the prompt built from
 *     the initial [CapsuleState]; stores the returned content plan JSON in
 *     [CapsuleState.contentPlanJson] and advances to
 *     [CapsuleStage.CONTEXT_PROPOSED].
 *  2. [validateContext] — pure — delegates to [ContentPlanValidator]. On
 *     [ContentPlanValidationResult.Valid] advances to
 *     [CapsuleStage.CONTEXT_VALIDATED] with `contextValid = true`. On
 *     [ContentPlanValidationResult.Invalid] advances to [CapsuleStage.FAILED]
 *     with the validator error.
 *  3. [generateSpeakerNotes] — calls [CapsuleLlm.generate] with the prompt
 *     built from the validated state; stores the enriched AsciiDoc in
 *     [CapsuleState.speakerNotesAdoc], derives [CapsuleState.ttsScript] via
 *     [TtsScriptDeriver] and advances to [CapsuleStage.CONTENT_GENERATED].
 *
 * Conditional edges:
 *  - `validateContext → generateSpeakerNotes  onCondition { it.contextValid }`
 *  - `validateContext → nodeFinish            onCondition { !it.contextValid }`
 *
 * Non-périmètre (v1): no retry loop on validate, no Checkpoints, no
 * Self-Reflection, no RAG inside the graph (RAG stays in the CAP-ARCH-2
 * augmented-context task — the graph consumes its rendered artefact).
 *
 * @param promptBuilder builds the LLM prompts from the running [CapsuleState].
 * @param llm           the LLM provider (production: adapter on codebase's
 *                      `LlmBuildService`; tests: a fake).
 */
class CapsulePipelineGraph(
    private val promptBuilder: CapsulePromptBuilder,
    private val llm: CapsuleLlm,
) {

    private val log = LoggerFactory.getLogger(CapsulePipelineGraph::class.java)

    val graph: AIAgentGraphStrategy<CapsuleState, CapsuleState> = strategy<CapsuleState, CapsuleState>(
        name = "capsule-pipeline",
        toolSelectionStrategy = ToolSelectionStrategy.NONE,
    ) {
        val proposeContext by node<CapsuleState, CapsuleState> { state ->
            proposeContextNode(state)
        }
        val validateContext by node<CapsuleState, CapsuleState> { state ->
            validateContextNode(state)
        }
        val generateSpeakerNotes by node<CapsuleState, CapsuleState> { state ->
            generateSpeakerNotesNode(state)
        }

        edge(nodeStart forwardTo proposeContext onCondition { _ -> true } transformed { it })
        edge(proposeContext forwardTo validateContext onCondition { _ -> true } transformed { it })
        edge(validateContext forwardTo generateSpeakerNotes onCondition { it.contextValid } transformed { it })
        edge(validateContext forwardTo nodeFinish onCondition { !it.contextValid } transformed { it })
        edge(generateSpeakerNotes forwardTo nodeFinish onCondition { _ -> true } transformed { it })
    }

    /**
     * Runs the pipeline sequentially from [initialState] to a final
     * [CapsuleState]. Each node is wrapped in try/catch; failures surface as
     * [CapsuleStage.FAILED] with a non-null [CapsuleState.error].
     */
    fun execute(initialState: CapsuleState): CapsuleState {
        var state = try {
            proposeContextNode(initialState)
        } catch (e: Exception) {
            log.warn("[CapsulePipelineGraph] propose-context failed: {}", e.message)
            initialState.copy(
                stage = CapsuleStage.FAILED,
                error = "ProposeContextFailed: ${e.message}",
            )
        }

        if (state.stage == CapsuleStage.FAILED) return state

        state = validateContextNode(state)
        if (!state.contextValid) return state

        return try {
            generateSpeakerNotesNode(state)
        } catch (e: Exception) {
            log.error("[CapsulePipelineGraph] generate-speaker-notes failed: {}", e.message)
            state.copy(
                stage = CapsuleStage.FAILED,
                error = "GenerateSpeakerNotesFailed: ${e.message}",
            )
        }
    }

    fun asMermaidDiagram(): String = runBlocking { graph.asMermaidDiagram() }

    private fun proposeContextNode(state: CapsuleState): CapsuleState {
        val prompt = promptBuilder.buildProposePrompt(state)
        val contentPlanJson = llm.propose(prompt)
        require(contentPlanJson.isNotBlank()) { "LLM returned a blank content plan JSON" }
        return state.copy(
            contentPlanJson = contentPlanJson,
            stage = CapsuleStage.CONTEXT_PROPOSED,
        )
    }

    private fun validateContextNode(state: CapsuleState): CapsuleState {
        return when (val result = ContentPlanValidator.validate(state.contentPlanJson)) {
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
    }

    private fun generateSpeakerNotesNode(state: CapsuleState): CapsuleState {
        val prompt = promptBuilder.buildGeneratePrompt(state)
        val speakerNotesAdoc = llm.generate(prompt)
        require(speakerNotesAdoc.isNotBlank()) { "LLM returned a blank speaker notes AsciiDoc" }
        return state.copy(
            speakerNotesAdoc = speakerNotesAdoc,
            ttsScript = TtsScriptDeriver.derive(speakerNotesAdoc, state.deckName),
            stage = CapsuleStage.CONTENT_GENERATED,
        )
    }
}
