package capsule.context

import contracts.context.ContextChannel
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [GlossaryLoader] and the [GlossaryEntry] / [GlossaryContext]
 * immutable models (CAP-GLOSSARY-1).
 *
 * The loader is a pure object mirroring [PedagogicalScenarioLoader]: it parses
 * an AsciiDoc `== Glossary` section (bullets `- term: definition`), renders a
 * prompt-ready `==== Official Glossary (glossary)` block, and truncates it to
 * the supplied token budget via the N0 [ContextChannel.Docs.truncateToTokens]
 * method (the Docs variant is semantically close — a glossary is a lightweight
 * documentary corpus).
 *
 * Missing files are skipped silently — a missing glossary yields an empty
 * string (backward compatible, no error). Malformed bullets (not
 * `- term: definition`) are skipped. Non-`Glossary` sections are ignored.
 */
class GlossaryLoaderTest {

    @TempDir
    lateinit var tempDir: File

    // ─── GlossaryEntry invariant ────────────────────────────────────────

    @Test
    fun `GlossaryEntry rejects a blank term`() {
        assertFailsWith<IllegalArgumentException> {
            GlossaryEntry(term = "", definition = "a definition")
        }
    }

    @Test
    fun `GlossaryEntry rejects a blank definition`() {
        assertFailsWith<IllegalArgumentException> {
            GlossaryEntry(term = "competence", definition = "  ")
        }
    }

    // ─── GlossaryContext invariant ──────────────────────────────────────

    @Test
    fun `GlossaryContext accepts an empty terms list`() {
        val ctx = GlossaryContext(terms = emptyList())
        assertEquals(0, ctx.terms.size)
    }

    @Test
    fun `GlossaryContext preserves the order of terms`() {
        val ctx = GlossaryContext(
            terms = listOf(
                GlossaryEntry("alpha", "first term"),
                GlossaryEntry("beta", "second term"),
                GlossaryEntry("gamma", "third term"),
            ),
        )
        assertEquals(listOf("alpha", "beta", "gamma"), ctx.terms.map { it.term })
    }

    // ─── GlossaryLoader load ───────────────────────────────────────────

    @Test
    fun `load with missing file returns blank string`() {
        val result = GlossaryLoader.load(File(tempDir, "missing-glossary.adoc"), 400)
        assertTrue(result.isBlank(), "Missing glossary file should yield a blank string")
    }

    @Test
    fun `load parses bullets and renders the Official Glossary section`() {
        val glossary = File(tempDir, "glossary.adoc").also {
            it.writeText(
                """
                = Glossaire FPA

                == Glossary
                - compétence transversale: savoir-faire mobilisable across contexts
                - évaluation formative: évaluation en cours d'apprentissage
                - modality: mode d'organisation d'une session
                """.trimIndent()
            )
        }
        val result = GlossaryLoader.load(glossary, 400)
        assertTrue(result.contains("Official Glossary (glossary)"), "Expected section header")
        assertTrue(result.contains("compétence transversale"), "Expected first term")
        assertTrue(result.contains("évaluation formative"), "Expected second term")
        assertTrue(result.contains("modality"), "Expected third term")
    }

    @Test
    fun `load with no Glossary section returns blank string`() {
        val glossary = File(tempDir, "no-glossary.adoc").also {
            it.writeText(
                """
                = Document without glossary

                == Objectifs
                - Goal A
                - Goal B
                """.trimIndent()
            )
        }
        val result = GlossaryLoader.load(glossary, 400)
        assertTrue(result.isBlank(), "Adoc without a `== Glossary` section should yield a blank string")
    }

    @Test
    fun `load with zero token budget returns blank string`() {
        val glossary = File(tempDir, "glossary.adoc").also {
            it.writeText("== Glossary\n- term: definition")
        }
        val result = GlossaryLoader.load(glossary, 0)
        assertTrue(result.isBlank(), "Zero token budget should yield a blank string")
    }

    @Test
    fun `load truncates content exceeding the token budget`() {
        val bullets = (1..80).joinToString("\n") { "- term$it: definition with enough words to consume the token budget" }
        val glossary = File(tempDir, "long.adoc").also {
            it.writeText("== Glossary\n$bullets")
        }
        val maxTokens = 50
        val result = GlossaryLoader.load(glossary, maxTokens)
        val estimated = ContextChannel.estimateTokens(result)
        assertTrue(estimated <= maxTokens + 50, "Result ($estimated tokens) should be truncated near budget ($maxTokens)")
        assertFalse(result.contains("term80"), "Truncated result should not contain the last term")
    }

    @Test
    fun `load with multiple terms preserves order in the rendered section`() {
        val glossary = File(tempDir, "ordered.adoc").also {
            it.writeText(
                """
                == Glossary
                - first: premier terme
                - second: deuxième terme
                - third: troisième terme
                """.trimIndent()
            )
        }
        val result = GlossaryLoader.load(glossary, 400)
        val firstIdx = result.indexOf("first")
        val secondIdx = result.indexOf("second")
        val thirdIdx = result.indexOf("third")
        assertTrue(firstIdx >= 0 && secondIdx > firstIdx && thirdIdx > secondIdx, "Terms order must be preserved")
    }

    @Test
    fun `load skips malformed bullets that are not term-definition pairs`() {
        val glossary = File(tempDir, "mixed.adoc").also {
            it.writeText(
                """
                == Glossary
                - valid term: a real definition
                - a line without colon
                - another valid: second definition
                """.trimIndent()
            )
        }
        val result = GlossaryLoader.load(glossary, 400)
        assertTrue(result.contains("valid term"), "Valid bullets must be kept")
        assertTrue(result.contains("another valid"), "Second valid bullet must be kept")
        assertFalse(result.contains("a line without colon"), "Malformed bullet without colon must be skipped")
    }

    @Test
    fun `load ignores non-Glossary sections`() {
        val glossary = File(tempDir, "mixed-sections.adoc").also {
            it.writeText(
                """
                = Document with multiple sections

                == Objectifs
                - Goal A

                == Glossary
                - glossary term: glossary definition

                == Prérequis
                - Prerequisite A
                """.trimIndent()
            )
        }
        val result = GlossaryLoader.load(glossary, 400)
        assertTrue(result.contains("glossary term"), "Glossary section must be parsed")
        assertFalse(result.contains("Goal A"), "Objectifs section must be ignored")
        assertFalse(result.contains("Prerequisite A"), "Prérequis section must be ignored")
    }

    @Test
    fun `load with empty Glossary section returns blank string`() {
        val glossary = File(tempDir, "empty-glossary.adoc").also {
            it.writeText(
                """
                = Document

                == Glossary

                == Objectifs
                - Goal A
                """.trimIndent()
            )
        }
        val result = GlossaryLoader.load(glossary, 400)
        assertTrue(result.isBlank(), "Empty Glossary section (no bullets) should yield a blank string")
    }

    @Test
    fun `load with blank adoc file returns blank string`() {
        val glossary = File(tempDir, "blank.adoc").also { it.writeText("") }
        val result = GlossaryLoader.load(glossary, 400)
        assertTrue(result.isBlank(), "Blank adoc should yield a blank string")
    }
}