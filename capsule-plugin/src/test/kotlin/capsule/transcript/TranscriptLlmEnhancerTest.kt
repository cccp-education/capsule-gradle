package capsule.transcript

import capsule.feed.SlideSegment
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for [TranscriptLlmEnhancer] + [ChatModelTranscriptEnhancer]
 * — CAP-TRANSCRIPT US-3.
 *
 * [TranscriptLlmEnhancer] is the domain port consumed by the
 * `generateCapsuleTranscript` task (US-4) when the [TranscriptStrategy] is
 * `LLM`. It enriches the deterministic template produced by
 * [TranscriptBuilder] with pedagogical transitions, introductions, and
 * reformulations. [ChatModelTranscriptEnhancer] is the production adapter
 * bridging the port to a langchain4j [ChatModel] (pattern
 * [capsule.pipeline.ChatModelCapsuleLlm]).
 *
 * Fallback degraded (pattern [capsule.audio.AudioPostProcessor.process]):
 * when the LLM returns a blank/whitespace-only response, the adapter
 * returns the original template unchanged — the caller keeps a valid
 * AsciiDoc article rather than an empty one.
 */
class TranscriptLlmEnhancerTest {

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

    private fun template(plan: TranscriptPlan = plan()): String =
        TranscriptBuilder.build(plan)

    /**
     * Minimal fake [ChatModel] returning a canned response — no network,
     * no key. Only [doChat] is overridden (the single abstract entry point
     * used by [ChatModelTranscriptEnhancer] via [ChatModel.chat]).
     */
    private class FakeChatModel(private val response: String) : ChatModel {
        var callCount = 0
            private set
        var lastPrompt: String? = null
            private set

        override fun doChat(request: ChatRequest): ChatResponse {
            callCount++
            lastPrompt = request.messages().lastOrNull { it is UserMessage }
                ?.let { (it as UserMessage).singleText() }
            return ChatResponse.builder()
                .aiMessage(AiMessage.from(response))
                .build()
        }
    }

    @Test
    fun `enhance returns the enriched article when the LLM responds`() {
        val enriched = "== Introduction\n\nWelcome, dear learner, to this capsule.\n"
        val enhancer = ChatModelTranscriptEnhancer(FakeChatModel(enriched))

        val result = enhancer.enhance(template(), plan())

        assertEquals(enriched, result)
    }

    @Test
    fun `enhance falls back to the original template when the LLM returns blank`() {
        val original = template()
        val enhancer = ChatModelTranscriptEnhancer(FakeChatModel("   "))

        val result = enhancer.enhance(original, plan())

        assertEquals(original, result, "blank LLM response should fall back to the original template")
    }

    @Test
    fun `enhance falls back to the original template when the LLM returns empty`() {
        val original = template()
        val enhancer = ChatModelTranscriptEnhancer(FakeChatModel(""))

        val result = enhancer.enhance(original, plan())

        assertEquals(original, result, "empty LLM response should fall back to the original template")
    }

    @Test
    fun `enhance forwards the enrichment prompt built by TranscriptPromptBuilder to the LLM`() {
        val fake = FakeChatModel("enriched body")
        val enhancer = ChatModelTranscriptEnhancer(fake)
        val plan = plan()

        enhancer.enhance(template(plan), plan)

        assertEquals(1, fake.callCount)
        assertTrue(
            fake.lastPrompt!!.contains("kotlin-basics"),
            "the forwarded prompt should embed the deck name"
        )
        assertTrue(
            fake.lastPrompt!!.contains("AsciiDoc", ignoreCase = true),
            "the forwarded prompt should be the enrichment prompt"
        )
    }

    @Test
    fun `enhance does not call the LLM when the template is already blank`() {
        val fake = FakeChatModel("enriched")
        val enhancer = ChatModelTranscriptEnhancer(fake)

        val result = enhancer.enhance("", plan())

        assertEquals("", result, "a blank template should short-circuit to blank without calling the LLM")
        assertEquals(0, fake.callCount)
    }

    @Test
    fun `enhance preserves the LLM response containing the template headings`() {
        val plan = plan()
        val original = template(plan)
        val enriched = "$original\n\n== Further Reading\n\nExtra section."
        val enhancer = ChatModelTranscriptEnhancer(FakeChatModel(enriched))

        val result = enhancer.enhance(original, plan)

        assertTrue(result.contains("== Introduction"), "enriched article should preserve the original headings")
        assertTrue(result.contains("== Further Reading"), "enriched article should include new LLM content")
    }
}