package capsule.pipeline

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultCapsulePromptBuilder] (CAP-SPD-2).
 *
 * The production prompt builder must emit a pedagogical SPD instruction line
 * when the running [CapsuleState] carries an SPD-augmented context. The SPD
 * section is already part of [CapsuleState.augmentedContext] (rendered by
 * [capsule.context.CapsuleContextBuilder] with the `spdSection` extension) —
 * the builder only references it textually, it never accesses
 * [capsule.context.SpdContext] directly.
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
    fun `buildGeneratePrompt with SPD-augmented context contains the SPD instruction line`() {
        val prompt = builder.buildGeneratePrompt(
            state(augmentedContext = "==== SPD Pedagogical Context (spd)\nSession: Bienvenue\nObjectives: Goal A"),
        )
        assertTrue(
            prompt.contains("SPD objectives"),
            "Generate prompt must reference SPD objectives when SPD context is present",
        )
        assertTrue(
            prompt.contains("Target duration"),
            "Generate prompt must reference SPD target duration",
        )
        assertTrue(
            prompt.contains("prerequisites"),
            "Generate prompt must reference SPD prerequisites",
        )
    }

    @Test
    fun `buildGeneratePrompt with empty augmented context falls back to generic prompt without SPD instruction`() {
        val prompt = builder.buildGeneratePrompt(state(augmentedContext = ""))
        assertTrue(prompt.contains("pedagogical scriptwriter"), "Generic instruction must remain")
        assertTrue(
            !prompt.contains("SPD objectives"),
            "Blank augmented context must not emit the SPD instruction line",
        )
    }

    @Test
    fun `buildGeneratePrompt with augmented context but no SPD section keeps generic prompt`() {
        val prompt = builder.buildGeneratePrompt(
            state(augmentedContext = "==== EAGER Context (EAGER/LAZY)\nINDEX.adoc content only"),
        )
        assertTrue(prompt.contains("pedagogical scriptwriter"))
        assertTrue(
            !prompt.contains("SPD Pedagogical Context"),
            "Augmented context without SPD section must not reference SPD",
        )
    }
}