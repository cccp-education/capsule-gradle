package capsule.viral

import capsule.feed.SlideSegment
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral hook generation domain (CAP-VIRAL US-4):
 * [ViralHookPlan], [ViralHookPromptBuilder], [ViralHookParser], [ViralHook],
 * [ViralHookGenerator] + [ChatModelViralHookGenerator].
 *
 * The hook/message/CTA generation is **anchored on the N1 augmented context**
 * carried by the plan (RAG/Graphify/Docs/EAGER) — the prompt embeds it, and the
 * parser tolerates prose/fences around the JSON, degrading to `null` when
 * nothing usable is returned.
 */
class ViralHookTest {

    private fun plan(
        deckName: String = "fpa-decouverte",
        language: String = "fr",
        platform: ViralPlatform = ViralPlatform.TIKTOK,
        duration: Int = 30,
        augmentedContext: String = "==== Docs (DOCS)\nLe référentiel FPA définit 4 activités types.",
    ) = ViralHookPlan(
        deckName = deckName,
        segments = listOf(
            SlideSegment(1, "Introduction", "Pourquoi devenir formateur ?"),
            SlideSegment(2, "Coeur", "Les 4 activités types du référentiel."),
        ),
        language = language,
        platform = platform,
        targetDurationSeconds = duration,
        augmentedContext = augmentedContext,
        outputPath = "build/capsule/$deckName-hook.json",
    )

    // ─── ViralHookPlan invariants ─────────────────────────────────────

    @Test
    fun `plan is anchored when the augmented context is non-blank`() {
        assertTrue(plan().isAnchored)
    }

    @Test
    fun `plan is not anchored when the augmented context is blank`() {
        assertFalse(plan(augmentedContext = "").isAnchored)
    }

    @Test
    fun `plan rejects a duration outside the short-form range`() {
        val below = runCatching { plan(duration = 10) }
        val above = runCatching { plan(duration = 90) }
        assertTrue(below.isFailure, "duration < 15 should be rejected")
        assertTrue(above.isFailure, "duration > 60 should be rejected")
    }

    @Test
    fun `plan rejects blank deck name language or output path`() {
        assertTrue(runCatching { plan(deckName = " ") }.isFailure)
        assertTrue(runCatching { plan(language = "") }.isFailure)
    }

    // ─── Prompt builder ───────────────────────────────────────────────

    @Test
    fun `hook prompt embeds the augmented context when anchored`() {
        val prompt = ViralHookPromptBuilder.buildHookPrompt(plan())
        assertTrue(prompt.contains("Le référentiel FPA définit 4 activités types."), "prompt should embed the N1 context")
        assertTrue(prompt.contains("Do not invent facts", ignoreCase = true), "prompt should forbid ungrounded claims")
        assertTrue(prompt.contains("fpa-decouverte"), "prompt should embed the deck name")
        assertTrue(prompt.contains("TIKTOK"), "prompt should embed the platform")
    }

    @Test
    fun `hook prompt warns when no augmented context is available`() {
        val prompt = ViralHookPromptBuilder.buildHookPrompt(plan(augmentedContext = ""))
        assertTrue(prompt.contains("No augmented context available", ignoreCase = true))
    }

    // ─── Parser ───────────────────────────────────────────────────────

    @Test
    fun `parser reads a strict JSON object`() {
        val hook = ViralHookParser.parse("""{"hook":"Et si 30 secondes suffisaient ?","message":"Devenir formateur","cta":"Découvrez FPA"}""")
        assertNotNull(hook)
        assertEquals("Et si 30 secondes suffisaient ?", hook.hook)
        assertEquals("Devenir formateur", hook.message)
        assertEquals("Découvrez FPA", hook.cta)
    }

    @Test
    fun `parser extracts JSON wrapped in prose and fences`() {
        val raw = """
            Voici le résultat :
            ```json
            {"hook":"Accroche","message":"Message","cta":"CTA"}
            ```
            Bonne journée.
        """.trimIndent()
        val hook = ViralHookParser.parse(raw)
        assertNotNull(hook)
        assertEquals("Accroche", hook.hook)
    }

    @Test
    fun `parser ignores braces inside strings`() {
        val raw = """{"hook":"Un {drôle} d'effet","message":"m","cta":"c"}"""
        val hook = ViralHookParser.parse(raw)
        assertNotNull(hook)
        assertEquals("Un {drôle} d'effet", hook.hook)
    }

    @Test
    fun `parser returns null on blank malformed or contentless input`() {
        assertNull(ViralHookParser.parse(""))
        assertNull(ViralHookParser.parse("   "))
        assertNull(ViralHookParser.parse("not json at all"))
        assertNull(ViralHookParser.parse("""{"hook":"","message":"","cta":""}"""))
    }

    @Test
    fun `parser tolerates partial fields`() {
        val hook = ViralHookParser.parse("""{"hook":"Accroche seule"}""")
        assertNotNull(hook)
        assertTrue(hook.hasHook)
        assertFalse(hook.hasCta)
    }

    // ─── ViralHook invariants ─────────────────────────────────────────

    @Test
    fun `ViralHook rejects an all-blank result`() {
        assertTrue(runCatching { ViralHook("", "", "") }.isFailure)
    }

    // ─── Adapter ──────────────────────────────────────────────────────

    private class FakeChatModel(private val response: String) : ChatModel {
        var callCount = 0
            private set
        var lastPrompt: String? = null
            private set

        override fun doChat(request: ChatRequest): ChatResponse {
            callCount++
            lastPrompt = request.messages().lastOrNull { it is UserMessage }
                ?.let { (it as UserMessage).singleText() }
            return ChatResponse.builder().aiMessage(AiMessage.from(response)).build()
        }
    }

    @Test
    fun `generator forwards the anchored prompt and parses the response`() {
        val fake = FakeChatModel("""{"hook":"Accroche","message":"Message ancré","cta":"CTA"}""")
        val generator = ChatModelViralHookGenerator(fake)
        val plan = plan()

        val hook = generator.generate(plan)

        assertNotNull(hook)
        assertEquals("Accroche", hook.hook)
        assertEquals(1, fake.callCount)
        assertTrue(fake.lastPrompt!!.contains("Le référentiel FPA définit 4 activités types."), "forwarded prompt should embed the N1 context")
    }

    @Test
    fun `generator returns null when the LLM response is unusable`() {
        val generator = ChatModelViralHookGenerator(FakeChatModel("désolé, je ne peux pas"))
        assertNull(generator.generate(plan()))
    }
}
