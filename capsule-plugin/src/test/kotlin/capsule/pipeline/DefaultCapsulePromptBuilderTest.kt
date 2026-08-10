package capsule.pipeline

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultCapsulePromptBuilder] (CAP-SPD-2).
 *
 * The production prompt builder must emit a pedagogical scenario instruction
 * line when the running [CapsuleState] carries a scenario-augmented context.
 * The scenario section is already part of [CapsuleState.augmentedContext]
 * (rendered by [capsule.context.CapsuleContextBuilder] with the
 * `scenarioSection` extension) — the builder only references it textually,
 * it never accesses [capsule.context.PedagogicalScenario] directly.
 */
class DefaultCapsulePromptBuilderTest {

    private val builder = DefaultCapsulePromptBuilder()

    private fun state(
        deckName: String = "demo",
        language: String = "fr",
        augmentedContext: String = "",
        contentPlanJson: String = """{"deckName":"demo","languageCode":"fr","slideCount":3,"speakerNotesFile":"demo-speaker-notes.adoc","ttsScriptFile":"demo-script.txt"}""",
    ): CapsuleState = CapsuleState(
        deckName = deckName,
        language = language,
        sourceAdoc = "= Demo\n\n== Slide A\nspeaker A\n",
        augmentedContext = augmentedContext,
        contentPlanJson = contentPlanJson,
        contextValid = true,
        stage = CapsuleStage.CONTEXT_PROPOSED,
    )

    @Test
    fun `buildGeneratePrompt with scenario-augmented context contains the scenario instruction line`() {
        val prompt = builder.buildGeneratePrompt(
            state(augmentedContext = "==== Pedagogical Scenario (scenario)\nSession: Bienvenue\nObjectives: Goal A"),
        )
        assertTrue(
            prompt.contains("pedagogical scenario objectives"),
            "Generate prompt must reference scenario objectives when scenario context is present",
        )
        assertTrue(
            prompt.contains("Target duration"),
            "Generate prompt must reference scenario target duration",
        )
        assertTrue(
            prompt.contains("prerequisites"),
            "Generate prompt must reference scenario prerequisites",
        )
    }

    @Test
    fun `buildGeneratePrompt with empty augmented context falls back to generic prompt without scenario instruction`() {
        val prompt = builder.buildGeneratePrompt(state(augmentedContext = ""))
        assertTrue(prompt.contains("pedagogical scriptwriter"), "Generic instruction must remain")
        assertTrue(
            !prompt.contains("pedagogical scenario objectives"),
            "Blank augmented context must not emit the scenario instruction line",
        )
    }

    @Test
    fun `buildGeneratePrompt with augmented context but no scenario section keeps generic prompt`() {
        val prompt = builder.buildGeneratePrompt(
            state(augmentedContext = "==== EAGER Context (EAGER/LAZY)\nINDEX.adoc content only"),
        )
        assertTrue(prompt.contains("pedagogical scriptwriter"))
        assertTrue(
            !prompt.contains("Pedagogical Scenario"),
            "Augmented context without scenario section must not reference the scenario",
        )
    }
}