package capsule.voice

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD unit tests for [NumberSpeller] (PRT-02 residual — numbers spelled for the
 * ASR fidelity comparison). Pure — no I/O.
 */
class NumberSpellerTest {

    @Test
    fun `spells small and compound numbers`() {
        assertEquals("zéro", NumberSpeller.french(0))
        assertEquals("un", NumberSpeller.french(1))
        assertEquals("seize", NumberSpeller.french(16))
        assertEquals("dix-sept", NumberSpeller.french(17))
        assertEquals("vingt et un", NumberSpeller.french(21))
        assertEquals("trente et un", NumberSpeller.french(31))
    }

    @Test
    fun `spells the seventy eighty ninety irregularities`() {
        assertEquals("soixante-dix", NumberSpeller.french(70))
        assertEquals("soixante et onze", NumberSpeller.french(71))
        assertEquals("soixante-douze", NumberSpeller.french(72))
        assertEquals("quatre-vingts", NumberSpeller.french(80))
        assertEquals("quatre-vingt-un", NumberSpeller.french(81))
        assertEquals("quatre-vingt-dix", NumberSpeller.french(90))
        assertEquals("quatre-vingt-onze", NumberSpeller.french(91))
    }

    @Test
    fun `spells hundreds with the plural rule`() {
        assertEquals("cent", NumberSpeller.french(100))
        assertEquals("cent un", NumberSpeller.french(101))
        assertEquals("deux cents", NumberSpeller.french(200))
        assertEquals("deux cent un", NumberSpeller.french(201))
        assertEquals("neuf cent quatre-vingt-dix-neuf", NumberSpeller.french(999))
    }

    @Test
    fun `spells thousands and invariable mille`() {
        assertEquals("mille", NumberSpeller.french(1000))
        assertEquals("deux mille", NumberSpeller.french(2000))
        assertEquals("deux mille vingt-quatre", NumberSpeller.french(2024))
    }

    @Test
    fun `spells integers inside a sentence and leaves the rest untouched`() {
        assertEquals("il y a quarante-deux pommes", NumberSpeller.spell("il y a 42 pommes"))
        assertEquals("aucun nombre ici", NumberSpeller.spell("aucun nombre ici"))
    }

    @Test
    fun `other languages are left unchanged`() {
        assertEquals("there are 42 apples", NumberSpeller.spell("there are 42 apples", language = "en"))
    }

    @Test
    fun `fidelity scoring matches digits against their spelled form`() {
        val score = FidelityScorer.score("il y a 42 pommes", "il y a quarante deux pommes")
        assertEquals(0.0, score.wordErrorRate, 1e-9)
    }
}
