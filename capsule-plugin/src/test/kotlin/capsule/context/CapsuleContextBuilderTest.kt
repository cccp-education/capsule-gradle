package capsule.context

import contracts.context.ChannelBudget
import contracts.context.ChannelType
import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import contracts.context.ContextChannel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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

    // ─── CAP-SPD-2: scenarioSection extension ──────────────────────────

    @Test
    fun `build with non-blank scenarioSection renders the scenario section after channels`() {
        val ctx = CapsuleContextBuilder.build(
            composite(eager = "EAGER governance"),
            scenarioSection = "==== Pedagogical Scenario (scenario)\nSession: Bienvenue\nObjectives: Goal A; Goal B",
        )
        assertTrue(ctx.rendered.contains("EAGER governance"), "channels must still be rendered")
        assertTrue(ctx.rendered.contains("Pedagogical Scenario"), "scenario section must be rendered")
        assertTrue(ctx.rendered.contains("Bienvenue"), "scenario content must be present")
    }

    @Test
    fun `build with blank scenarioSection drops the scenario section`() {
        val ctx = CapsuleContextBuilder.build(composite(eager = "EAGER only"), scenarioSection = "")
        assertTrue(ctx.rendered.contains("EAGER only"))
        assertTrue(!ctx.rendered.contains("Pedagogical Scenario"), "blank scenarioSection must not add a scenario header")
    }

    @Test
    fun `build with empty channels and non-blank scenarioSection renders only the scenario section`() {
        val ctx = CapsuleContextBuilder.build(
            composite(),
            scenarioSection = "==== Pedagogical Scenario (scenario)\nSession: Solo scenario",
        )
        assertTrue(ctx.isEmpty, "channels list must be empty")
        assertTrue(ctx.rendered.isNotBlank(), "rendered must carry the scenario section")
        assertTrue(ctx.rendered.contains("Solo scenario"))
    }

    @Test
    fun `CapsuleContext with no channels but non-blank scenarioSection accepts non-blank rendered`() {
        val ctx = CapsuleContext(
            channels = emptyList(),
            rendered = "==== Pedagogical Scenario (scenario)\nSession: only scenario",
            scenarioSection = "==== Pedagogical Scenario (scenario)\nSession: only scenario",
        )
        assertTrue(ctx.isEmpty)
        assertTrue(ctx.rendered.isNotBlank())
    }

    @Test
    fun `CapsuleContext invariant rejects non-blank rendered with no channels and blank scenarioSection`() {
        assertFailsWith<IllegalArgumentException> {
            CapsuleContext(channels = emptyList(), rendered = "orphan text", scenarioSection = "")
        }
    }

    @Test
    fun `build with both channels and scenarioSection coexist in rendered`() {
        val ctx = CapsuleContextBuilder.build(
            composite(eager = "EAGER content", rag = "RAG snippet"),
            scenarioSection = "==== Pedagogical Scenario (scenario)\nSession: Combined",
        )
        assertEquals(2, ctx.nonEmptyCount)
        assertTrue(ctx.rendered.contains("EAGER content"))
        assertTrue(ctx.rendered.contains("RAG snippet"))
        assertTrue(ctx.rendered.contains("Pedagogical Scenario"))
    }

    // ─── CAP-PROVENANCE: tracker extension ─────────────────────────────

    @Test
    fun `build with null tracker performs no provenance tracking`() {
        val ctx = CapsuleContextBuilder.build(composite(eager = "EAGER governance"))
        assertEquals(1, ctx.nonEmptyCount)
        assertTrue(ctx.rendered.contains("EAGER governance"))
    }

    @Test
    fun `build with tracker tracks the EAGER channel that survived the budget`() {
        val tracker = ProvenanceTracker()
        tracker.trackChannel(
            "EAGER",
            listOf(ProvenanceSource(fileName = "INDEX.adoc", chars = 120, tokens = 30)),
        )
        val ctx = CapsuleContextBuilder.build(composite(eager = "EAGER governance"), tracker = tracker)
        assertEquals(1, ctx.nonEmptyCount)
        val provenance = tracker.build()
        assertEquals(listOf("EAGER"), provenance.channels.map { it.channel })
    }

    @Test
    fun `build with tracker tracks the scenario channel when scenarioSection is non-blank`() {
        val tracker = ProvenanceTracker()
        tracker.trackChannel(
            "SCENARIO",
            listOf(ProvenanceSource(fileName = "session.adoc", chars = 200, tokens = 40)),
        )
        val ctx = CapsuleContextBuilder.build(
            composite(),
            scenarioSection = "==== Pedagogical Scenario (scenario)\nSession: Solo scenario",
            tracker = tracker,
        )
        assertTrue(ctx.isEmpty)
        val provenance = tracker.build()
        assertEquals(listOf("SCENARIO"), provenance.channels.map { it.channel })
    }

    // ─── CAP-GLOSSARY: glossarySection extension ────────────────────────

    @Test
    fun `build with non-blank glossarySection renders the glossary section after scenario section`() {
        val ctx = CapsuleContextBuilder.build(
            composite(eager = "EAGER governance"),
            scenarioSection = "==== Pedagogical Scenario (scenario)\nSession: Bienvenue",
            glossarySection = "==== Official Glossary (glossary)\ncompétence: savoir-faire",
        )
        assertTrue(ctx.rendered.contains("EAGER governance"), "channels must still be rendered")
        assertTrue(ctx.rendered.contains("Pedagogical Scenario"), "scenario section must be rendered")
        assertTrue(ctx.rendered.contains("Official Glossary"), "glossary section must be rendered")
        assertTrue(ctx.rendered.contains("compétence"), "glossary content must be present")
        val scenarioIdx = ctx.rendered.indexOf("Pedagogical Scenario")
        val glossaryIdx = ctx.rendered.indexOf("Official Glossary")
        assertTrue(glossaryIdx > scenarioIdx, "Glossary section must appear after scenario section")
    }

    @Test
    fun `build with blank glossarySection drops the glossary section`() {
        val ctx = CapsuleContextBuilder.build(
            composite(eager = "EAGER only"),
            glossarySection = "",
        )
        assertTrue(ctx.rendered.contains("EAGER only"))
        assertFalse(ctx.rendered.contains("Official Glossary"), "blank glossarySection must not add a glossary header")
    }

    @Test
    fun `build with empty channels and non-blank glossarySection renders only the glossary section`() {
        val ctx = CapsuleContextBuilder.build(
            composite(),
            glossarySection = "==== Official Glossary (glossary)\ncompétence: savoir-faire",
        )
        assertTrue(ctx.isEmpty, "channels list must be empty")
        assertTrue(ctx.rendered.isNotBlank(), "rendered must carry the glossary section")
        assertTrue(ctx.rendered.contains("Official Glossary"))
    }

    @Test
    fun `CapsuleContext with no channels but non-blank glossarySection accepts non-blank rendered`() {
        val ctx = CapsuleContext(
            channels = emptyList(),
            rendered = "==== Official Glossary (glossary)\ncompétence: savoir-faire",
            scenarioSection = "",
            glossarySection = "==== Official Glossary (glossary)\ncompétence: savoir-faire",
        )
        assertTrue(ctx.isEmpty)
        assertTrue(ctx.rendered.isNotBlank())
    }

    @Test
    fun `CapsuleContext invariant rejects non-blank rendered with no channels, blank scenario and blank glossary`() {
        assertFailsWith<IllegalArgumentException> {
            CapsuleContext(channels = emptyList(), rendered = "orphan text", scenarioSection = "", glossarySection = "")
        }
    }

    @Test
    fun `build with channels, scenario and glossary all coexisting in rendered`() {
        val ctx = CapsuleContextBuilder.build(
            composite(eager = "EAGER content", rag = "RAG snippet"),
            scenarioSection = "==== Pedagogical Scenario (scenario)\nSession: Combined",
            glossarySection = "==== Official Glossary (glossary)\nterm: definition",
        )
        assertEquals(2, ctx.nonEmptyCount)
        assertTrue(ctx.rendered.contains("EAGER content"))
        assertTrue(ctx.rendered.contains("RAG snippet"))
        assertTrue(ctx.rendered.contains("Pedagogical Scenario"))
        assertTrue(ctx.rendered.contains("Official Glossary"))
    }

    @Test
    fun `build with tracker tracks the glossary channel when glossarySection is non-blank`() {
        val tracker = ProvenanceTracker()
        tracker.trackChannel(
            "GLOSSARY",
            listOf(ProvenanceSource(fileName = "glossary.adoc", chars = 300, tokens = 60)),
        )
        val ctx = CapsuleContextBuilder.build(
            composite(),
            glossarySection = "==== Official Glossary (glossary)\nterm: definition",
            tracker = tracker,
        )
        assertTrue(ctx.isEmpty)
        val provenance = tracker.build()
        assertEquals(listOf("GLOSSARY"), provenance.channels.map { it.channel })
    }
}
