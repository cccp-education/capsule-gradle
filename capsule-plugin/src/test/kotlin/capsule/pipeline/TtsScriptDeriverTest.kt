package capsule.pipeline

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [TtsScriptDeriver] — pure derivation of the TTS script from
 * enriched speaker notes AsciiDoc (CAP-ARCH-3, reuses the `capsule.feed`
 * round-trip).
 */
class TtsScriptDeriverTest {

    @Test
    fun `derives a canonical script from speaker notes with NOTE blocks`() {
        val adoc = """
            == Slide 1

            [NOTE.speaker]
            --
            First enriched narration.
            --

            == Slide 2

            [NOTE.speaker]
            --
            Second enriched narration.
            --
        """.trimIndent()

        val script = TtsScriptDeriver.derive(adoc, "kotlin-coroutines")

        assertTrue(script.startsWith("=== CAPSULE SCRIPT : kotlin-coroutines ==="))
        assertTrue(script.contains("--- SLIDE 1 : Slide 1 ---"))
        assertTrue(script.contains("First enriched narration."))
        assertTrue(script.contains("--- SLIDE 2 : Slide 2 ---"))
        assertTrue(script.contains("Second enriched narration."))
    }

    @Test
    fun `returns a blank script when no NOTE speaker block is present`() {
        val adoc = """
            == Slide 1

            Some text without a speaker note block.

            == Slide 2

            Also no block here.
        """.trimIndent()

        val script = TtsScriptDeriver.derive(adoc, "kotlin-coroutines")

        assertTrue(script.isEmpty())
    }

    @Test
    fun `returns a blank script for an empty adoc`() {
        assertEquals("", TtsScriptDeriver.derive("", "kotlin-coroutines"))
    }

    @Test
    fun `rejects a blank deckName`() {
        try {
            TtsScriptDeriver.derive("== Slide 1", "  ")
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("deckName"))
        }
    }
}
