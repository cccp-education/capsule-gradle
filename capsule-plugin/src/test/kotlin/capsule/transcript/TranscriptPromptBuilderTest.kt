package capsule.transcript

import capsule.feed.SlideSegment
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * TDD unit tests for [TranscriptPromptBuilder] — CAP-TRANSCRIPT US-2.
 *
 * [TranscriptPromptBuilder] is a pure object that builds the pedagogical
 * prompt sent to the LLM when the [TranscriptStrategy] is `LLM`. The prompt
 * asks the LLM to enrich the template AsciiDoc article with pedagogical
 * transitions, keep the `== ` headings and speaker note content, add
 * introductory and concluding paragraphs, and respond with AsciiDoc only.
 * Pattern: [capsule.pipeline.DefaultCapsulePromptBuilder].
 */
class TranscriptPromptBuilderTest {

    private fun plan(
        deckName: String = "kotlin-basics",
        language: String = "fr",
    ): TranscriptPlan = TranscriptPlan(
        deckName = deckName,
        segments = listOf(
            SlideSegment(1, "Introduction", "Welcome to the capsule."),
            SlideSegment(2, "Core", "The core concept."),
        ),
        language = language,
        outputPath = "build/capsule/$deckName-transcript.adoc",
    )

    @Test
    fun `buildEnhancePrompt mentions the deck name`() {
        val prompt = TranscriptPromptBuilder.buildEnhancePrompt(plan(deckName = "kotlin-basics"))
        assertTrue(prompt.contains("kotlin-basics"), "prompt should mention the deck name")
    }

    @Test
    fun `buildEnhancePrompt mentions the language`() {
        val prompt = TranscriptPromptBuilder.buildEnhancePrompt(plan(language = "fr"))
        assertTrue(prompt.contains("fr"), "prompt should mention the language code")
    }

    @Test
    fun `buildEnhancePrompt includes the template article for enrichment`() {
        val prompt = TranscriptPromptBuilder.buildEnhancePrompt(plan())
        assertTrue(prompt.contains("== Introduction"), "prompt should include the template article sections")
        assertTrue(prompt.contains("Welcome to the capsule."), "prompt should include the template speaker notes")
    }

    @Test
    fun `buildEnhancePrompt instructs to keep headings and respond AsciiDoc only`() {
        val prompt = TranscriptPromptBuilder.buildEnhancePrompt(plan())
        assertTrue(prompt.contains("== ", ignoreCase = false) || prompt.contains("headings", ignoreCase = true),
            "prompt should instruct to keep '==' headings")
        assertTrue(prompt.contains("transitions", ignoreCase = true), "prompt should ask for pedagogical transitions")
        assertTrue(prompt.contains("AsciiDoc", ignoreCase = true), "prompt should mention AsciiDoc")
        assertTrue(prompt.contains("only", ignoreCase = true), "prompt should ask for AsciiDoc only")
    }

    @Test
    fun `buildEnhancePrompt instructs to preserve speaker note content`() {
        val prompt = TranscriptPromptBuilder.buildEnhancePrompt(plan())
        assertTrue(prompt.contains("speaker", ignoreCase = true) || prompt.contains("content", ignoreCase = true),
            "prompt should instruct to preserve speaker note content")
    }
}