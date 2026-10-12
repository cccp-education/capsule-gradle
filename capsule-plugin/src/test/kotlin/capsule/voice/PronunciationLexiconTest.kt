package capsule.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for [PronunciationLexicon] (PRT-01). Pure — no I/O.
 */
class PronunciationLexiconTest {

    @Test
    fun `acronyms are spelled for the voice`() {
        assertEquals("the A P I is ready", PronunciationLexicon.spoken("the API is ready"))
    }

    @Test
    fun `symbols are expanded for the voice`() {
        assertEquals("50 pour cent et 3 euros", PronunciationLexicon.spoken("50% & 3€"))
    }

    @Test
    fun `plain text is unchanged`() {
        assertEquals("bonjour le monde", PronunciationLexicon.spoken("bonjour le monde"))
    }

    @Test
    fun `reversals map the spoken word back to the symbol`() {
        val reversals = PronunciationLexicon.reversals()
        assertEquals("%", reversals["pour cent"])
        assertEquals("€", reversals["euros"])
    }

    @Test
    fun `the cache key is a stable eight hex characters`() {
        val key = PronunciationLexicon.cacheKey("bonjour")
        assertEquals(8, key.length)
        assertTrue(key.matches(Regex("[0-9a-f]{8}")), "key should be hex, got $key")
        assertEquals(key, PronunciationLexicon.cacheKey("bonjour"))
    }

    @Test
    fun `a different spoken text yields a different key`() {
        assertNotEquals(
            PronunciationLexicon.cacheKey("bonjour"),
            PronunciationLexicon.cacheKey("bonjour le monde"),
        )
    }
}
