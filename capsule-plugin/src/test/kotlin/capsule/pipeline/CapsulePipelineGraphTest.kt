package capsule.pipeline

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CapsulePipelineGraph] — the koog-orchestrated content
 * generation pipeline (CAP-ARCH-3).
 *
 * Three nodes (propose-context → validate-context → generate-speaker-notes)
 * chained by conditional edges. The LLM is mocked via [FakeCapsulePromptBuilder]
 * + [FakeCapsuleLlm] (no network, no key). The validator is the real
 * [ContentPlanValidator] (pure domain service).
 *
 * Baby-step TDD: graph topology + happy path + failure modes (pattern
 * `slider.pipeline.DeckPipelineGraphTest` SLD-8.3).
 */
class CapsulePipelineGraphTest {

    private val validContentPlanJson = """
        {
          "deckName": "kotlin-coroutines",
          "languageCode": "fr",
          "slideCount": 24,
          "speakerNotesFile": "kotlin-coroutines-speaker-notes.adoc",
          "ttsScriptFile": "kotlin-coroutines-script.txt"
        }
    """.trimIndent()

    private val enrichedSpeakerNotes = """
        == Introduction

        [NOTE.speaker]
        --
        Enriched intro narration.
        --

        == Details

        [NOTE.speaker]
        --
        Enriched details narration.
        --
    """.trimIndent()

    private fun initialState(
        deckName: String = "kotlin-coroutines",
        language: String = "fr",
    ): CapsuleState = CapsuleState(
        deckName = deckName,
        language = language,
        sourceAdoc = "== Slide 1\n\n[NOTE.speaker]\n--\nBase narration.\n--",
        augmentedContext = "RAG nugget about grading.",
        contentPlanJson = "",
    )

    @Test
    fun `execute returns CONTENT_GENERATED on the happy path`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(
                proposeResponse = validContentPlanJson,
                generateResponse = enrichedSpeakerNotes,
            ),
        )

        val result = graph.execute(initialState())

        assertEquals(CapsuleStage.CONTENT_GENERATED, result.stage)
        assertEquals(enrichedSpeakerNotes, result.speakerNotesAdoc)
        assertTrue(result.contextValid)
        assertNull(result.validationError)
        assertNull(result.error)
    }

    @Test
    fun `execute populates contentPlanJson from the propose-context node`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes),
        )

        val result = graph.execute(initialState())

        assertEquals(validContentPlanJson, result.contentPlanJson)
    }

    @Test
    fun `execute derives the TTS script from the generated speaker notes`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes),
        )

        val result = graph.execute(initialState())

        assertTrue(result.ttsScript.startsWith("=== CAPSULE SCRIPT : kotlin-coroutines ==="))
        assertTrue(result.ttsScript.contains("Enriched intro narration."))
        assertTrue(result.ttsScript.contains("Enriched details narration."))
    }

    @Test
    fun `execute stops at FAILED when the LLM proposes an invalid content plan`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(
                proposeResponse = """{"deckName": "missing fields"}""",
                generateResponse = "== Should not be reached",
            ),
        )

        val result = graph.execute(initialState())

        assertEquals(CapsuleStage.FAILED, result.stage)
        assertFalse(result.contextValid)
        assertNotNull(result.validationError)
        assertNotNull(result.error)
        assertTrue(result.speakerNotesAdoc.isEmpty())
    }

    @Test
    fun `execute reports the validator error when the content plan is invalid`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(proposeResponse = "", generateResponse = ""),
        )

        val result = graph.execute(initialState())

        assertEquals(CapsuleStage.FAILED, result.stage)
        assertTrue(result.error.orEmpty().contains("blank"))
    }

    @Test
    fun `execute fails the pipeline when the propose-context LLM throws`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(proposeException = RuntimeException("LLM unavailable")),
        )

        val result = graph.execute(initialState())

        assertEquals(CapsuleStage.FAILED, result.stage)
        assertNotNull(result.error)
        assertTrue(result.contentPlanJson.isEmpty())
    }

    @Test
    fun `execute fails the pipeline when the generate-speaker-notes LLM throws`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(
                proposeResponse = validContentPlanJson,
                generateException = RuntimeException("Generation failed"),
            ),
        )

        val result = graph.execute(initialState())

        assertEquals(CapsuleStage.FAILED, result.stage)
        assertNotNull(result.error)
        assertTrue(result.contextValid)
        assertTrue(result.speakerNotesAdoc.isEmpty())
    }

    @Test
    fun `execute calls the prompt builder with the running state for propose`() {
        val promptBuilder = FakeCapsulePromptBuilder()
        val graph = CapsulePipelineGraph(
            promptBuilder = promptBuilder,
            llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes),
        )

        graph.execute(initialState())

        assertEquals(1, promptBuilder.proposeCalls.size)
        assertEquals("kotlin-coroutines", promptBuilder.proposeCalls[0].deckName)
    }

    @Test
    fun `execute calls the prompt builder with the validated state for generate`() {
        val promptBuilder = FakeCapsulePromptBuilder()
        val graph = CapsulePipelineGraph(
            promptBuilder = promptBuilder,
            llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes),
        )

        graph.execute(initialState())

        assertEquals(1, promptBuilder.generateCalls.size)
        val generateState = promptBuilder.generateCalls[0]
        assertTrue(generateState.contextValid)
        assertEquals(validContentPlanJson, generateState.contentPlanJson)
    }

    @Test
    fun `execute does not call the generate prompt when the content plan is invalid`() {
        val promptBuilder = FakeCapsulePromptBuilder()
        val graph = CapsulePipelineGraph(
            promptBuilder = promptBuilder,
            llm = FakeCapsuleLlm(proposeResponse = "{}", generateResponse = "== Should not be reached"),
        )

        graph.execute(initialState())

        assertTrue(promptBuilder.generateCalls.isEmpty())
    }

    @Test
    fun `execute does not call the generate LLM when the content plan is invalid`() {
        val llm = FakeCapsuleLlm(proposeResponse = "{}", generateResponse = "== Should not be reached")
        val graph = CapsulePipelineGraph(promptBuilder = FakeCapsulePromptBuilder(), llm = llm)

        graph.execute(initialState())

        assertEquals(0, llm.generateCallCount)
    }

    @Test
    fun `execute forwards the propose prompt to the LLM provider`() {
        val llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes)
        val graph = CapsulePipelineGraph(promptBuilder = FakeCapsulePromptBuilder(), llm = llm)

        graph.execute(initialState())

        assertEquals(1, llm.proposePrompts.size)
        assertTrue(llm.proposePrompts[0].contains("kotlin-coroutines"))
    }

    @Test
    fun `execute forwards the generate prompt to the LLM provider`() {
        val llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes)
        val graph = CapsulePipelineGraph(promptBuilder = FakeCapsulePromptBuilder(), llm = llm)

        graph.execute(initialState())

        assertEquals(1, llm.generatePrompts.size)
    }

    @Test
    fun `asMermaidDiagram returns a non-blank mermaid graph description`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes),
        )

        val mermaid = graph.asMermaidDiagram()

        assertTrue(mermaid.isNotBlank())
    }

    @Test
    fun `execute preserves the initial deck name and language through the pipeline`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes),
        )

        val result = graph.execute(initialState(deckName = "reactive-streams", language = "en"))

        assertEquals("reactive-streams", result.deckName)
        assertEquals("en", result.language)
    }

    @Test
    fun `execute preserves the augmented context through the pipeline`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes),
        )

        val result = graph.execute(initialState().copy(augmentedContext = "custom RAG context"))

        assertEquals("custom RAG context", result.augmentedContext)
    }

    @Test
    fun `execute transitions through CONTEXT_PROPOSED before CONTEXT_VALIDATED`() {
        val promptBuilder = TrackingPromptBuilder()
        val graph = CapsulePipelineGraph(
            promptBuilder = promptBuilder,
            llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = enrichedSpeakerNotes),
        )

        graph.execute(initialState())

        assertEquals(CapsuleStage.INITIALIZED, promptBuilder.proposeStates[0].stage)
        assertEquals(CapsuleStage.CONTEXT_VALIDATED, promptBuilder.generateStates[0].stage)
    }

    @Test
    fun `execute sets stage FAILED when generate throws despite a valid context`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(
                proposeResponse = validContentPlanJson,
                generateException = IllegalStateException("timeout"),
            ),
        )

        val result = graph.execute(initialState())

        assertEquals(CapsuleStage.FAILED, result.stage)
        assertTrue(result.contextValid)
        assertTrue(result.error.orEmpty().contains("timeout"))
    }

    @Test
    fun `propose LLM returning a blank content plan fails fast`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(proposeResponse = "", generateResponse = enrichedSpeakerNotes),
        )

        val result = graph.execute(initialState())

        assertEquals(CapsuleStage.FAILED, result.stage)
        assertTrue(result.error.orEmpty().contains("blank"))
    }

    @Test
    fun `generate LLM returning a blank adoc fails fast`() {
        val graph = CapsulePipelineGraph(
            promptBuilder = FakeCapsulePromptBuilder(),
            llm = FakeCapsuleLlm(proposeResponse = validContentPlanJson, generateResponse = ""),
        )

        val result = graph.execute(initialState())

        assertEquals(CapsuleStage.FAILED, result.stage)
        assertTrue(result.error.orEmpty().contains("blank"))
    }

    /**
     * Deterministic prompt builder stub — records the states it sees and
     * returns prompts derived from the deck name so tests can assert on them.
     */
    private class FakeCapsulePromptBuilder : CapsulePromptBuilder {
        val proposeCalls = mutableListOf<CapsuleState>()
        val generateCalls = mutableListOf<CapsuleState>()

        override fun buildProposePrompt(state: CapsuleState): String {
            proposeCalls.add(state)
            return "Propose a content plan JSON for deck='${state.deckName}' in language='${state.language}'."
        }

        override fun buildGeneratePrompt(state: CapsuleState): String {
            generateCalls.add(state)
            return "Generate enriched speaker notes from this content plan: ${state.contentPlanJson}."
        }
    }

    /**
     * Prompt builder that captures the full [CapsuleState] at each call point
     * to verify stage transitions without exposing intermediate state.
     */
    private class TrackingPromptBuilder : CapsulePromptBuilder {
        val proposeStates = mutableListOf<CapsuleState>()
        val generateStates = mutableListOf<CapsuleState>()

        override fun buildProposePrompt(state: CapsuleState): String {
            proposeStates.add(state)
            return "propose"
        }

        override fun buildGeneratePrompt(state: CapsuleState): String {
            generateStates.add(state)
            return "generate"
        }
    }

    /**
     * Fake LLM — no network, no key. Returns canned responses (or throws).
     */
    private class FakeCapsuleLlm(
        private val proposeResponse: String = "",
        private val generateResponse: String = "",
        private val proposeException: RuntimeException? = null,
        private val generateException: RuntimeException? = null,
    ) : CapsuleLlm {
        val proposePrompts = mutableListOf<String>()
        val generatePrompts = mutableListOf<String>()
        var proposeCallCount = 0
            private set
        var generateCallCount = 0
            private set

        override fun propose(prompt: String): String {
            proposeCallCount++
            proposePrompts.add(prompt)
            proposeException?.let { throw it }
            return proposeResponse
        }

        override fun generate(prompt: String): String {
            generateCallCount++
            generatePrompts.add(prompt)
            generateException?.let { throw it }
            return generateResponse
        }
    }
}
