package capsule.transcript

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD unit tests for [TranscriptStrategy] enum (CAP-TRANSCRIPT US-1).
 *
 * `TranscriptStrategy` is a type-safe enum with two values:
 * `TEMPLATE` (default, backward compat — assemble AsciiDoc from segments,
 * no LLM required) and `LLM` (enrich the template via
 * `TranscriptLlmEnhancer`, requires `LlmBuildService`). The companion
 * `fromString` is case-insensitive and falls back to `TEMPLATE` for
 * unknown/blank values.
 */
class TranscriptStrategyTest {

    @Test
    fun `fromString returns TEMPLATE for null`() {
        assertEquals(TranscriptStrategy.TEMPLATE, TranscriptStrategy.fromString(null))
    }

    @Test
    fun `fromString returns TEMPLATE for blank`() {
        assertEquals(TranscriptStrategy.TEMPLATE, TranscriptStrategy.fromString(""))
        assertEquals(TranscriptStrategy.TEMPLATE, TranscriptStrategy.fromString("   "))
    }

    @Test
    fun `fromString returns TEMPLATE for known value`() {
        assertEquals(TranscriptStrategy.TEMPLATE, TranscriptStrategy.fromString("template"))
    }

    @Test
    fun `fromString returns LLM for known value`() {
        assertEquals(TranscriptStrategy.LLM, TranscriptStrategy.fromString("llm"))
    }

    @Test
    fun `fromString is case-insensitive`() {
        assertEquals(TranscriptStrategy.TEMPLATE, TranscriptStrategy.fromString("Template"))
        assertEquals(TranscriptStrategy.TEMPLATE, TranscriptStrategy.fromString("TEMPLATE"))
        assertEquals(TranscriptStrategy.LLM, TranscriptStrategy.fromString("Llm"))
        assertEquals(TranscriptStrategy.LLM, TranscriptStrategy.fromString("LLM"))
    }

    @Test
    fun `fromString falls back to TEMPLATE for unknown value`() {
        assertEquals(TranscriptStrategy.TEMPLATE, TranscriptStrategy.fromString("ai"))
        assertEquals(TranscriptStrategy.TEMPLATE, TranscriptStrategy.fromString("foobar"))
    }

    @Test
    fun `enum has exactly two values`() {
        assertEquals(2, TranscriptStrategy.entries.size)
        assertEquals(TranscriptStrategy.TEMPLATE, TranscriptStrategy.entries[0])
        assertEquals(TranscriptStrategy.LLM, TranscriptStrategy.entries[1])
    }
}