package capsule.context

import contracts.context.ChannelBudget
import contracts.context.ContextChannel
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * TDD unit tests for [DocContextLoader] (CAP-DOCCONTEXT-2).
 *
 * The loader is an object pur that takes pre-resolved files (glob resolution
 * happens in the wiring layer via Gradle `fileTree`), reads + concatenates
 * them with `--- filename ---` headers, and truncates the result to the Docs
 * channel token budget (`budget.docsTokens` = 10% of total).
 */
class DocContextLoaderTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `load with empty file list returns blank string`() {
        val result = DocContextLoader.load(emptyList(), ChannelBudget())
        assertTrue(result.isBlank(), "Empty file list should yield a blank string")
    }

    @Test
    fun `load with a single file returns its content with a header`() {
        val file = File(tempDir, "afnor-guide.adoc").also { it.writeText("AFNOR grading rules.") }
        val result = DocContextLoader.load(listOf(file), ChannelBudget())
        assertTrue(result.contains("--- afnor-guide.adoc ---"), "Expected file header")
        assertTrue(result.contains("AFNOR grading rules."), "Expected file content")
    }

    @Test
    fun `load with multiple files concatenates them sorted by name`() {
        val fileB = File(tempDir, "reac-module.adoc").also { it.writeText("REAC content B.") }
        val fileA = File(tempDir, "afnor-guide.adoc").also { it.writeText("AFNOR content A.") }
        val result = DocContextLoader.load(listOf(fileB, fileA), ChannelBudget())
        val idxAfnor = result.indexOf("afnor-guide.adoc")
        val idxReac = result.indexOf("reac-module.adoc")
        assertTrue(idxAfnor < idxReac, "Files should be sorted by name (afnor before reac)")
    }

    @Test
    fun `load truncates content exceeding the docs token budget`() {
        val bigContent = (1..200).joinToString("\n") { "line $it with enough words to consume tokens" }
        val file = File(tempDir, "big.adoc").also { it.writeText(bigContent) }
        val budget = ChannelBudget(totalTokenBudget = 100, budgetEager = 0.0, budgetRag = 0.0, budgetGraphify = 0.0, budgetDocs = 1.0, budgetResource = 0.0)
        val result = DocContextLoader.load(listOf(file), budget)
        val maxTokens = budget.docsTokens
        val estimatedTokens = ContextChannel.estimateTokens(result)
        assertTrue(estimatedTokens <= maxTokens + 50, "Result ($estimatedTokens tokens) should be truncated near budget ($maxTokens tokens), not full 200 lines")
        assertFalse(result.contains("line 200"), "Truncated result should not contain the last line")
    }

    @Test
    fun `load with zero docs budget returns blank string`() {
        val file = File(tempDir, "afnor.adoc").also { it.writeText("Some content.") }
        val budget = ChannelBudget(totalTokenBudget = 8000, budgetEager = 0.40, budgetRag = 0.30, budgetGraphify = 0.20, budgetDocs = 0.0, budgetResource = 0.10)
        val result = DocContextLoader.load(listOf(file), budget)
        assertTrue(result.isBlank(), "Zero docs budget should yield a blank string")
    }

    @Test
    fun `load skips non-existent files gracefully`() {
        val existing = File(tempDir, "exists.adoc").also { it.writeText("Real content.") }
        val missing = File(tempDir, "missing.adoc")
        val result = DocContextLoader.load(listOf(existing, missing), ChannelBudget())
        assertTrue(result.contains("Real content."), "Existing file content should be present")
        assertFalse(result.contains("missing.adoc"), "Missing file should be skipped (no header)")
    }

    @Test
    fun `load reads markdown and txt files alongside adoc`() {
        val adoc = File(tempDir, "guide.adoc").also { it.writeText("AsciiDoc content.") }
        val md = File(tempDir, "notes.md").also { it.writeText("Markdown content.") }
        val txt = File(tempDir, "raw.txt").also { it.writeText("Plain text content.") }
        val result = DocContextLoader.load(listOf(adoc, md, txt), ChannelBudget())
        assertTrue(result.contains("AsciiDoc content."), "Should read .adoc")
        assertTrue(result.contains("Markdown content."), "Should read .md")
        assertTrue(result.contains("Plain text content."), "Should read .txt")
    }

    @Test
    fun `load with empty content files returns blank string`() {
        val emptyFile = File(tempDir, "empty.adoc").also { it.writeText("") }
        val blankFile = File(tempDir, "blank.adoc").also { it.writeText("   \n  ") }
        val result = DocContextLoader.load(listOf(emptyFile, blankFile), ChannelBudget())
        assertTrue(result.isBlank(), "All-blank files should yield a blank string")
    }

    @Test
    fun `load does not truncate content under the budget`() {
        val smallContent = "Short AFNOR guide."
        val file = File(tempDir, "small.adoc").also { it.writeText(smallContent) }
        val result = DocContextLoader.load(listOf(file), ChannelBudget())
        assertTrue(result.contains(smallContent), "Small content under budget should be preserved in full")
    }
}