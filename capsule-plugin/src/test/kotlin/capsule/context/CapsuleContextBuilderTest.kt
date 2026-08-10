package capsule.context

import contracts.context.ChannelBudget
import contracts.context.ChannelType
import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import contracts.context.ContextChannel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [CapsuleContextBuilder] — the `capsule.context` augmented
 * context assembler (CAP-ARCH-2).
 *
 * Baby-step TDD (RED → GREEN): the builder is a pure object consuming the N0
 * `codebase-contracts` context contract (`CompositeContext` +
 * `ContextChannel` + `ChannelBudget`) and rendering it into the prompt-ready
 * block that will feed the CAP-ARCH-3 koog pipeline. No I/O, no Gradle, no
 * network.
 */
class CapsuleContextBuilderTest {

    private fun composite(
        eager: String = "",
        rag: String = "",
        graphify: String = "",
        docs: String = "",
        config: CompositeContextConfig = CompositeContextConfig(),
    ): CompositeContext = CompositeContext(eager, rag, graphify, docs, config)

    // ─── toChannels ─────────────────────────────────────────────────────

    @Test
    fun `toChannels returns the 5 canonical channels in EAGER RAG GRAPHIFY DOCS RESOURCE order`() {
        val channels = CapsuleContextBuilder.toChannels(composite("eager", "rag", "graphify", "docs"))
        assertEquals(5, channels.size)
        assertEquals(
            listOf(
                ChannelType.EAGER,
                ChannelType.RAG,
                ChannelType.GRAPHIFY,
                ChannelType.DOCS,
                ChannelType.RESOURCE,
            ),
            channels.map { it.type },
        )
    }

    @Test
    fun `toChannels preserves the section content of each channel`() {
        val channels = CapsuleContextBuilder.toChannels(composite("eager", "rag", "graphify", "docs"))
        assertEquals("eager", channels[0].content)
        assertEquals("rag", channels[1].content)
        assertEquals("graphify", channels[2].content)
        assertEquals("docs", channels[3].content)
        assertTrue(channels[4].content.isBlank())
    }

    // ─── channelsWithBudget ─────────────────────────────────────────────

    @Test
    fun `channelsWithBudget truncates a channel exceeding its token budget`() {
        val bigRag = (1..100).joinToString("\n") { "line number $it with some words to consume tokens" }
        val channels = CapsuleContextBuilder.channelsWithBudget(
            composite(rag = bigRag),
            ChannelBudget(
                totalTokenBudget = 100,
                budgetEager = 0.0,
                budgetRag = 1.0,
                budgetGraphify = 0.0,
                budgetDocs = 0.0,
                budgetResource = 0.0,
            ),
        )
        val rag = channels.single { it.type == ChannelType.RAG }
        assertTrue(rag.content.length < bigRag.length, "RAG content should have been truncated")
        assertTrue(ContextChannel.estimateTokens(rag.content) <= 100, "RAG content must fit the budget")
    }

    @Test
    fun `channelsWithBudget preserves content already under budget`() {
        val shortRag = "a short rag snippet"
        val channels = CapsuleContextBuilder.channelsWithBudget(
            composite(rag = shortRag),
            ChannelBudget(
                totalTokenBudget = 8000,
                budgetEager = 0.0,
                budgetRag = 1.0,
                budgetGraphify = 0.0,
                budgetDocs = 0.0,
                budgetResource = 0.0,
            ),
        )
        val rag = channels.single { it.type == ChannelType.RAG }
        assertTrue(rag.content.startsWith(shortRag), "content should be preserved, got: '${rag.content}'")
        assertEquals(shortRag, rag.content.trim())
    }

    // ─── merge ──────────────────────────────────────────────────────────

    @Test
    fun `merge produces a sectioned block with header and channel name`() {
        val rendered = CapsuleContextBuilder.merge(
            listOf(ContextChannel.Eager("rules about grading"))
        )
        assertTrue(rendered.contains("RÈGLES_EAGER"), "expected section header")
        assertTrue(rendered.contains("EAGER/LAZY"), "expected channel name")
        assertTrue(rendered.contains("rules about grading"), "expected content")
    }

    @Test
    fun `merge drops blank channels`() {
        val rendered = CapsuleContextBuilder.merge(
            listOf(ContextChannel.Eager(""), ContextChannel.Rag("real rag"), ContextChannel.Graphify(""))
        )
        assertTrue(!rendered.contains("RÈGLES_EAGER"), "blank EAGER channel must be dropped")
        assertTrue(rendered.contains("CONTEXTE_RAG"), "non-blank RAG channel must be kept")
        assertTrue(rendered.contains("real rag"))
    }

    @Test
    fun `merge of only blank channels yields a blank block`() {
        assertTrue(CapsuleContextBuilder.merge(listOf(ContextChannel.Eager(""), ContextChannel.Docs(""))).isBlank())
    }

    // ─── build ──────────────────────────────────────────────────────────

    @Test
    fun `build keeps only non-empty channels and renders them`() {
        val ctx = CapsuleContextBuilder.build(composite(eager = "EAGER governance", rag = "RAG snippet"))
        assertEquals(2, ctx.nonEmptyCount)
        assertEquals(2, ctx.channels.size)
        assertTrue(ctx.rendered.contains("EAGER governance"))
        assertTrue(ctx.rendered.contains("RAG snippet"))
    }

    @Test
    fun `build of an all-empty composite yields an empty CapsuleContext`() {
        val ctx = CapsuleContextBuilder.build(composite())
        assertTrue(ctx.isEmpty)
        assertTrue(ctx.rendered.isBlank())
    }

    @Test
    fun `build applies the token budget to the channels`() {
        val bigRag = (1..100).joinToString("\n") { "sentence number $it" }
        val ctx = CapsuleContextBuilder.build(
            composite(eager = "eager", rag = bigRag),
            ChannelBudget(
                totalTokenBudget = 60,
                budgetEager = 0.0,
                budgetRag = 1.0,
                budgetGraphify = 0.0,
                budgetDocs = 0.0,
                budgetResource = 0.0,
            ),
        )
        val rag = ctx.channels.single { it.type == ChannelType.RAG }
        assertTrue(rag.content.length < bigRag.length, "build must truncate RAG to the budget")
    }

    @Test
    fun `build of a partially empty composite renders only the populated channels`() {
        val ctx = CapsuleContextBuilder.build(composite(eager = "EAGER only"))
        assertEquals(1, ctx.nonEmptyCount)
        assertTrue(!ctx.rendered.contains("CONTEXTE_RAG"))
    }

    // ─── CapsuleContext domain ──────────────────────────────────────────

    @Test
    fun `tokenEstimate derives from the rendered block`() {
        val ctx = CapsuleContextBuilder.build(composite(eager = "a"))
        assertEquals(ContextChannel.estimateTokens(ctx.rendered), ctx.tokenEstimate)
    }

    @Test
    fun `CapsuleContext invariant rejects a non-blank rendered block with zero channels`() {
        assertFailsWith<IllegalArgumentException> {
            CapsuleContext(channels = emptyList(), rendered = "orphan text")
        }
    }

    // ─── default budget ─────────────────────────────────────────────────

    @Test
    fun `defaultBudget is the N0 8000-token proportional budget`() {
        val budget = CapsuleContextBuilder.defaultBudget()
        assertEquals(ContextChannel.DEFAULT_TOKEN_BUDGET, budget.totalTokenBudget)
        assertEquals(0.40, budget.budgetEager)
        assertEquals(0.30, budget.budgetRag)
    }

    // ─── CAP-SPD-2: spdSection extension ───────────────────────────────

    @Test
    fun `build with non-blank spdSection renders the SPD section after channels`() {
        val ctx = CapsuleContextBuilder.build(
            composite(eager = "EAGER governance"),
            spdSection = "==== SPD Pedagogical Context (spd)\nSession: Bienvenue\nObjectives: Goal A; Goal B",
        )
        assertTrue(ctx.rendered.contains("EAGER governance"), "channels must still be rendered")
        assertTrue(ctx.rendered.contains("SPD Pedagogical Context"), "SPD section must be rendered")
        assertTrue(ctx.rendered.contains("Bienvenue"), "SPD content must be present")
    }

    @Test
    fun `build with blank spdSection drops the SPD section`() {
        val ctx = CapsuleContextBuilder.build(composite(eager = "EAGER only"), spdSection = "")
        assertTrue(ctx.rendered.contains("EAGER only"))
        assertTrue(!ctx.rendered.contains("SPD Pedagogical Context"), "blank spdSection must not add an SPD header")
    }

    @Test
    fun `build with empty channels and non-blank spdSection renders only the SPD section`() {
        val ctx = CapsuleContextBuilder.build(
            composite(),
            spdSection = "==== SPD Pedagogical Context (spd)\nSession: Solo SPD",
        )
        assertTrue(ctx.isEmpty, "channels list must be empty")
        assertTrue(ctx.rendered.isNotBlank(), "rendered must carry the SPD section")
        assertTrue(ctx.rendered.contains("Solo SPD"))
    }

    @Test
    fun `CapsuleContext with no channels but non-blank spdSection accepts non-blank rendered`() {
        val ctx = CapsuleContext(
            channels = emptyList(),
            rendered = "==== SPD Pedagogical Context (spd)\nSession: only SPD",
            spdSection = "==== SPD Pedagogical Context (spd)\nSession: only SPD",
        )
        assertTrue(ctx.isEmpty)
        assertTrue(ctx.rendered.isNotBlank())
    }

    @Test
    fun `CapsuleContext invariant rejects non-blank rendered with no channels and blank spdSection`() {
        assertFailsWith<IllegalArgumentException> {
            CapsuleContext(channels = emptyList(), rendered = "orphan text", spdSection = "")
        }
    }

    @Test
    fun `build with both channels and spdSection coexist in rendered`() {
        val ctx = CapsuleContextBuilder.build(
            composite(eager = "EAGER content", rag = "RAG snippet"),
            spdSection = "==== SPD Pedagogical Context (spd)\nSession: Combined",
        )
        assertEquals(2, ctx.nonEmptyCount)
        assertTrue(ctx.rendered.contains("EAGER content"))
        assertTrue(ctx.rendered.contains("RAG snippet"))
        assertTrue(ctx.rendered.contains("SPD Pedagogical Context"))
    }
}
