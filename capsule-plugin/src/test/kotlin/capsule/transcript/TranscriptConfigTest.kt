package capsule.transcript

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * TDD unit tests for [TranscriptConfig] — CAP-TRANSCRIPT US-1.
 *
 * The config is a plain data class with 2 fields and backward-compat
 * defaults (all disabled — existing configs without a `transcript`
 * section keep the no-transcript behavior): `enabled` defaults to
 * `false` (opt-in) and `strategy` defaults to `TEMPLATE` (no LLM
 * required, deterministic).
 */
class TranscriptConfigTest {

    @Test
    fun `TranscriptConfig defaults are backward-compat disabled`() {
        val config = TranscriptConfig()
        assertFalse(config.enabled, "enabled should default to false (transcript is opt-in)")
        assertEquals(TranscriptStrategy.TEMPLATE, config.strategy, "strategy should default to TEMPLATE")
    }

    @Test
    fun `TranscriptConfig can enable with explicit LLM strategy`() {
        val config = TranscriptConfig(enabled = true, strategy = TranscriptStrategy.LLM)
        assertEquals(true, config.enabled)
        assertEquals(TranscriptStrategy.LLM, config.strategy)
    }

    @Test
    fun `TranscriptConfig can enable with TEMPLATE strategy`() {
        val config = TranscriptConfig(enabled = true, strategy = TranscriptStrategy.TEMPLATE)
        assertEquals(true, config.enabled)
        assertEquals(TranscriptStrategy.TEMPLATE, config.strategy)
    }

    @Test
    fun `TranscriptConfig is a data class with equals by value`() {
        val a = TranscriptConfig(enabled = true, strategy = TranscriptStrategy.LLM)
        val b = TranscriptConfig(enabled = true, strategy = TranscriptStrategy.LLM)
        assertEquals(a, b, "data class equals should be by value")
    }

    @Test
    fun `TranscriptConfig copy preserves unmodified fields`() {
        val base = TranscriptConfig(enabled = true, strategy = TranscriptStrategy.LLM)
        val copy = base.copy(enabled = false)
        assertFalse(copy.enabled)
        assertEquals(TranscriptStrategy.LLM, copy.strategy, "strategy preserved on copy")
    }
}