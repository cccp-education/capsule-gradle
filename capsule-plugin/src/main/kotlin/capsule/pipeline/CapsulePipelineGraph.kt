package capsule.pipeline

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import codebase.koog.pipeline.pipelineGraph

/**
 * Koog-orchestrated capsule content generation pipeline — the heart of
 * EPIC CAP-ARCH-3.
 *
 * EPIC KOOG-PIPELINE-DEDUP (BS-2 adoption): this class is now a thin facade
 * over the shared N1 skeleton `codebase.koog.pipeline.PipelineGraph`. All the
 * capsule-specific node logic lives in [CapsulePipelineDefinition]; the koog
 * topology (3 nodes with the conditional `validate -> generate | nodeFinish`
 * edges) and the sequential fail-fast `execute` are inherited from the N1
 * skeleton — the exact same structure previously duplicated alongside
 * `slider.pipeline.DeckPipelineGraph`.
 *
 * Architecture ("koog orchestrates, langchain4j executes"):
 *  - [graph] is a koog [AIAgentGraphStrategy] declaring 3 nodes wired by
 *    conditional edges. It captures the *topology* of the pipeline and is
 *    queryable via [asMermaidDiagram].
 *  - [execute] runs the pipeline sequentially with per-node try/catch, keeping
 *    failure modes explicit and the unit tests free from koog's async runtime.
 *
 * Conditional edges (topology):
 *  - `validate -> generate    onCondition { it.contextValid }`
 *  - `validate -> nodeFinish  onCondition { !it.contextValid }`
 *
 * Failures surface as [CapsuleStage.FAILED] with a non-null
 * [CapsuleState.error] (`ProposeContextFailed:` / `GenerateSpeakerNotesFailed:`).
 *
 * @param promptBuilder builds the LLM prompts from the running [CapsuleState].
 * @param llm           the LLM provider (production: adapter on codebase's
 *                      `LlmBuildService`; tests: a fake).
 */
class CapsulePipelineGraph(
    promptBuilder: CapsulePromptBuilder,
    llm: CapsuleLlm,
) {

    private val delegate = pipelineGraph(CapsulePipelineDefinition(promptBuilder, llm))

    val graph: AIAgentGraphStrategy<CapsuleState, CapsuleState> = delegate.graph

    /**
     * Runs the pipeline sequentially from [initialState] to a final
     * [CapsuleState]. Each node is wrapped in try/catch; failures surface as
     * [CapsuleStage.FAILED] with a non-null [CapsuleState.error].
     */
    fun execute(initialState: CapsuleState): CapsuleState = delegate.execute(initialState)

    fun asMermaidDiagram(): String = delegate.asMermaidDiagram()
}
