package capsule.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD unit tests for [TakeSelector] and [VoiceSelectionJson] (CAP-QC-VOICE US-3).
 * Pure — no I/O.
 */
class TakeSelectorTest {

    private val thresholds = VoiceThresholds()

    private fun candidate(
        slideIndex: Int,
        audio: String,
        expected: String,
        transcript: String,
        deck: String = "demo",
    ): TakeVerification {
        val score = FidelityScorer.score(expected, transcript)
        return TakeVerification(
            deckName = deck,
            slideIndex = slideIndex,
            audioFile = audio,
            expectedText = expected,
            transcript = transcript,
            wordErrorRate = score.wordErrorRate,
            surplus = score.surplus,
            snrDb = 70.0,
            verdict = VoiceQualityGate.verdict(score, 70.0, thresholds),
        )
    }

    private fun degraded(slideIndex: Int, audio: String): TakeVerification = TakeVerification(
        deckName = "demo",
        slideIndex = slideIndex,
        audioFile = audio,
        expectedText = "bonjour",
        transcript = "",
        wordErrorRate = null,
        surplus = null,
        snrDb = null,
        verdict = VoiceVerdict.Degraded(VoiceVerifier.REASON_ASR_ERROR),
    )

    @Test
    fun `the lowest word error rate wins`() {
        val good = candidate(1, "slide-01-1.mp3", "le chat noir", "le chat noir")
        val bad = candidate(1, "slide-01-2.mp3", "le chat noir", "le chien blanc")

        assertEquals("slide-01-1.mp3", TakeSelector.selectBest(listOf(bad, good))!!.audioFile)
    }

    @Test
    fun `a non-failing take beats a failing one`() {
        val pass = candidate(1, "slide-01-1.mp3", "a b c", "a b c")
        val fail = candidate(1, "slide-01-2.mp3", "a b c", "x y z w v u")

        assertEquals("slide-01-1.mp3", TakeSelector.selectBest(listOf(fail, pass))!!.audioFile)
    }

    @Test
    fun `a verified take beats a degraded one`() {
        val verified = candidate(1, "slide-01-1.mp3", "bonjour", "bonjour")
        val deg = degraded(1, "slide-01-2.mp3")

        assertEquals("slide-01-1.mp3", TakeSelector.selectBest(listOf(deg, verified))!!.audioFile)
    }

    @Test
    fun `ties break on the lowest surplus then on input order`() {
        // Same word error rate (1/3); the first has one extra word (surplus 1), the second 0.
        val surplusOne = candidate(1, "slide-01-1.mp3", "a b c", "a b c d")
        val surplusZero = candidate(1, "slide-01-2.mp3", "a b c", "a x c")
        assertEquals("slide-01-2.mp3", TakeSelector.selectBest(listOf(surplusOne, surplusZero))!!.audioFile)

        // Strict tie -> first in input order.
        val first = candidate(1, "slide-01-1.mp3", "a b c", "a b c d")
        val second = candidate(1, "slide-01-2.mp3", "a b c", "a b c d")
        assertEquals("slide-01-1.mp3", TakeSelector.selectBest(listOf(first, second))!!.audioFile)
    }

    @Test
    fun `selecting an empty candidate list is null`() {
        assertNull(TakeSelector.selectBest(emptyList()))
    }

    @Test
    fun `selectBySlide groups per deck and slide in order`() {
        val selections = TakeSelector.selectBySlide(
            listOf(
                candidate(2, "b-slide-02-1.mp3", "trois", "trois"),
                candidate(1, "a-slide-01-1.mp3", "un", "un"),
                candidate(1, "a-slide-01-2.mp3", "un", "deux"),
            ).map { it.copy(deckName = if (it.audioFile.startsWith("a")) "a" else "b") }
        )

        assertEquals(listOf("a" to 1, "b" to 2), selections.map { it.deckName to it.slideIndex })
        val a = selections.first()
        assertEquals("a-slide-01-1.mp3", a.chosenAudioFile)
        assertEquals(2, a.candidateCount)
        assertEquals("PASS", a.verdict)
    }

    @Test
    fun `selection json renders the chosen take`() {
        val selection = VoiceSelection(
            deckName = "demo",
            slideIndex = 1,
            chosenAudioFile = "slide-01-2.mp3",
            candidateCount = 2,
            wordErrorRate = 0.0,
            surplus = 0,
            verdict = "PASS",
        )
        val json = VoiceSelectionJson.toJson(listOf(selection))
        val node = com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get(0)

        assertEquals("demo", node.get("deckName").asText())
        assertEquals("slide-01-2.mp3", node.get("chosenAudioFile").asText())
        assertEquals(2, node.get("candidateCount").asInt())
        assertEquals("PASS", node.get("verdict").asText())
    }

    @Test
    fun `an empty selection renders an empty json array`() {
        assertEquals("[]", VoiceSelectionJson.toJson(emptyList()))
    }
}
