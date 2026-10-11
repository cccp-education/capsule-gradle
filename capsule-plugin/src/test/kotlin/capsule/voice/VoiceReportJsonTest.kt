package capsule.voice

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [VoiceReportJson] (CAP-QC-VOICE US-2). Pure — no I/O.
 */
class VoiceReportJsonTest {

    private val mapper = ObjectMapper()
    private val thresholds = VoiceThresholds(maxWordErrorRate = 0.15, minSnrDb = 66.0, surplusThreshold = 3)

    private fun take(index: Int, expected: String, transcript: String, snr: Double? = 70.0): TakeVerification {
        val score = FidelityScorer.score(expected, transcript)
        return TakeVerification(
            deckName = "demo",
            slideIndex = index,
            audioFile = "slide-%02d.mp3".format(index),
            expectedText = expected,
            transcript = transcript,
            wordErrorRate = score.wordErrorRate,
            surplus = score.surplus,
            snrDb = snr,
            verdict = VoiceQualityGate.verdict(score, snr, thresholds),
        )
    }

    @Test
    fun `a passing report renders deterministic json`() {
        val report = VoiceReport(
            degraded = false,
            takes = listOf(take(1, "bonjour le monde", "bonjour le monde")),
            thresholds = thresholds,
        )

        val json = VoiceReportJson.toJson(report)
        val root = mapper.readTree(json)

        assertFalse(root.get("degraded").asBoolean())
        assertEquals(0, root.get("failureCount").asInt())
        val first = root.get("takes").get(0)
        assertEquals("demo", first.get("deckName").asText())
        assertEquals(1, first.get("slideIndex").asInt())
        assertEquals("PASS", first.get("verdict").asText())
        assertEquals(0, first.get("reasons").size())
    }

    @Test
    fun `a failing take carries its reason codes`() {
        val report = VoiceReport(
            degraded = false,
            takes = listOf(take(1, "a b c", "a b c d e f")),
            thresholds = thresholds,
        )

        val first = mapper.readTree(VoiceReportJson.toJson(report)).get("takes").get(0)
        assertEquals("FAIL", first.get("verdict").asText())
        val reasons = first.get("reasons").map { it.asText() }
        assertTrue(VoiceQualityGate.REASON_SURPLUS in reasons)
    }

    @Test
    fun `a degraded take renders null metrics`() {
        val degradedTake = TakeVerification(
            deckName = "demo",
            slideIndex = 1,
            audioFile = "slide-01.mp3",
            expectedText = "bonjour",
            transcript = "",
            wordErrorRate = null,
            surplus = null,
            snrDb = null,
            verdict = VoiceVerdict.Degraded(VoiceVerifier.REASON_ASR_UNAVAILABLE),
        )
        val report = VoiceReport(degraded = true, takes = listOf(degradedTake), thresholds = thresholds)

        val first = mapper.readTree(VoiceReportJson.toJson(report)).get("takes").get(0)
        assertEquals("DEGRADED", first.get("verdict").asText())
        assertTrue(first.get("wordErrorRate").isNull)
        assertTrue(first.get("snrDb").isNull)
        assertEquals(listOf(VoiceVerifier.REASON_ASR_UNAVAILABLE), first.get("reasons").map { it.asText() })
    }
}
