package capsule.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * TDD unit tests for [LlmResilience]: a guarded LLM call degrades to the caller's
 * fallback on any exception (transient cloud error), but never swallows an [Error].
 */
class LlmResilienceTest {

    @Test
    fun `returns the block result on success`() {
        assertEquals(42, LlmResilience.guarded("op", -1) { 42 })
    }

    @Test
    fun `returns the fallback when the block throws a runtime exception`() {
        assertEquals(-1, LlmResilience.guarded("op", -1) { throw RuntimeException("too many concurrent requests") })
    }

    @Test
    fun `returns the fallback when the block throws an illegal state`() {
        assertEquals("fallback", LlmResilience.guarded("op", "fallback") { error("boom") })
    }

    @Test
    fun `does not swallow errors`() {
        assertFailsWith<AssertionError> {
            LlmResilience.guarded("op", 0) { throw AssertionError("fatal") }
        }
    }
}
