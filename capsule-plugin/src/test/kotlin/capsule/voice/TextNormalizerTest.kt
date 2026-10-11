package capsule.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for [TextNormalizer] (CAP-QC-VOICE US-1). Pure — no I/O.
 */
class TextNormalizerTest {

    @Test
    fun `folds case accents and punctuation`() {
        assertEquals(listOf("ete", "coeur"), TextNormalizer.normalize("Été — Cœur!"))
    }

    @Test
    fun `expands oe and ae ligatures`() {
        assertEquals(listOf("soeur", "caecum"), TextNormalizer.normalize("sœur cæcum"))
    }

    @Test
    fun `turns apostrophes and hyphens into spaces`() {
        assertEquals(listOf("l", "ete", "peut", "etre"), TextNormalizer.normalize("l'été peut-être"))
    }

    @Test
    fun `keeps digits`() {
        assertEquals(listOf("2x", "42"), TextNormalizer.normalize("2x = 42"))
    }

    @Test
    fun `blank text yields no word`() {
        assertTrue(TextNormalizer.normalize("   — !!!").isEmpty())
    }

    @Test
    fun `normalization is idempotent on its output`() {
        val once = TextNormalizer.normalize("Déjà Vu").joinToString(" ")
        assertEquals(once, TextNormalizer.normalize(once).joinToString(" "))
    }
}
