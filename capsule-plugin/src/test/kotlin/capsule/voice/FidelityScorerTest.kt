package capsule.voice

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD unit tests for [FidelityScorer] (CAP-QC-VOICE US-1). Pure — no I/O.
 */
class FidelityScorerTest {

    @Test
    fun `an exact match scores a zero word error rate`() {
        val score = FidelityScorer.score("bonjour le monde", "bonjour le monde")
        assertEquals(0.0, score.wordErrorRate, 1e-9)
        assertEquals(0, score.distance)
        assertEquals(0, score.surplus)
        assertEquals(3, score.referenceWordCount)
    }

    @Test
    fun `extra transcribed words raise the surplus`() {
        val score = FidelityScorer.score("a b c", "a b c d")
        assertEquals(1, score.surplus)
        assertEquals(1, score.distance)
        assertEquals(1.0 / 3.0, score.wordErrorRate, 1e-9)
    }

    @Test
    fun `a substitution raises the word error rate without surplus`() {
        val score = FidelityScorer.score("le chat noir", "le chien noir")
        assertEquals(0, score.surplus)
        assertEquals(1.0 / 3.0, score.wordErrorRate, 1e-9)
    }

    @Test
    fun `comparison is insensitive to case accents and punctuation`() {
        val score = FidelityScorer.score("Été", "ete!")
        assertEquals(0.0, score.wordErrorRate, 1e-9)
    }

    @Test
    fun `an empty reference against one word yields word error rate one`() {
        val score = FidelityScorer.score("", "hallucination")
        assertEquals(1.0, score.wordErrorRate, 1e-9)
        assertEquals(1, score.surplus)
        assertEquals(0, score.referenceWordCount)
    }
}
