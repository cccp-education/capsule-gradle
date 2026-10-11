package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for [RenderSignature] (PRT-06). Pure — no I/O.
 */
class RenderSignatureTest {

    @Test
    fun `signature is stable for identical inputs`() {
        val a = RenderSignature.of("screenshot", "deck", "5.0")
        val b = RenderSignature.of("screenshot", "deck", "5.0")
        assertEquals(a, b)
    }

    @Test
    fun `signature changes when the engine changes`() {
        assertNotEquals(
            RenderSignature.of("screenshot", "deck", "5.0"),
            RenderSignature.of("remotion", "deck", "5.0"),
        )
    }

    @Test
    fun `signature changes when the content changes`() {
        assertNotEquals(
            RenderSignature.of("screenshot", "deck-v1", "5.0"),
            RenderSignature.of("screenshot", "deck-v2", "5.0"),
        )
    }

    @Test
    fun `signature changes when the timeline changes`() {
        assertNotEquals(
            RenderSignature.of("screenshot", "deck", "5.0"),
            RenderSignature.of("screenshot", "deck", "6.0"),
        )
    }

    @Test
    fun `signature is a fixed-length lowercase hex string`() {
        val sig = RenderSignature.of("screenshot", "deck", "5.0")
        assertEquals(RenderSignature.LENGTH, sig.length)
        assertTrue(sig.matches(Regex("[0-9a-f]+")), "expected lowercase hex, got $sig")
    }
}
