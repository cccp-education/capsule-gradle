package capsule.voice

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [VoiceVerifier] (CAP-QC-VOICE US-2). Fake ASR — no I/O.
 */
class VoiceVerifierTest {

    private val thresholds = VoiceThresholds(maxWordErrorRate = 0.15, minSnrDb = 66.0, surplusThreshold = 3)

    private fun take(index: Int, expected: String) =
        VoiceTake(deckName = "deck", slideIndex = index, expectedText = expected, audioFile = File("slide-%02d.mp3".format(index)))

    @Test
    fun `a faithful take passes`() {
        val fake = SpeechRecognizer { _, _ -> "bonjour le monde" }
        val report = VoiceVerifier.verify(listOf(take(1, "bonjour le monde")), fake, thresholds, "fr")

        assertFalse(report.degraded)
        assertFalse(report.hasFailure)
        assertEquals(VoiceVerdict.Pass, report.takes.single().verdict)
        assertEquals(0.0, report.takes.single().wordErrorRate)
    }

    @Test
    fun `a hallucinated take fails with a surplus reason`() {
        val fake = SpeechRecognizer { _, _ -> "a b c d e f" }
        val report = VoiceVerifier.verify(listOf(take(1, "a b c")), fake, thresholds, "fr")

        assertTrue(report.hasFailure)
        val verdict = report.takes.single().verdict
        assertTrue(verdict is VoiceVerdict.Fail)
        assertTrue(VoiceQualityGate.REASON_SURPLUS in verdict.reasons)
    }

    @Test
    fun `the envelope provider feeds the snr check`() {
        val fake = SpeechRecognizer { _, _ -> "bonjour" }
        val report = VoiceVerifier.verify(
            listOf(take(1, "bonjour")),
            fake,
            thresholds,
            "fr",
            envelopeProvider = { DoubleArray(10) { 1.0 } },
        )

        val verdict = report.takes.single().verdict
        assertTrue(verdict is VoiceVerdict.Fail)
        assertTrue(VoiceQualityGate.REASON_SNR in verdict.reasons)
        assertEquals(0.0, report.takes.single().snrDb)
    }

    @Test
    fun `a recognition error degrades only that take`() {
        val failing = SpeechRecognizer { _, _ -> throw IllegalStateException("whisper crashed") }
        val report = VoiceVerifier.verify(listOf(take(1, "bonjour")), failing, thresholds, "fr")

        assertFalse(report.degraded)
        assertFalse(report.hasFailure)
        assertEquals(
            VoiceVerdict.Degraded(VoiceVerifier.REASON_ASR_ERROR),
            report.takes.single().verdict,
        )
    }

    @Test
    fun `the degraded factory marks every take degraded without a failure`() {
        val report = VoiceVerifier.degraded(listOf(take(1, "a"), take(2, "b")), thresholds)

        assertTrue(report.degraded)
        assertFalse(report.hasFailure)
        assertTrue(report.takes.all { it.verdict is VoiceVerdict.Degraded })
    }

    @Test
    fun `the report keeps the take order and identity`() {
        val fake = SpeechRecognizer { audio, _ -> if (audio.name == "slide-01.mp3") "un deux" else "trois quatre" }
        val report = VoiceVerifier.verify(listOf(take(2, "trois quatre"), take(1, "un deux")), fake, thresholds, "fr")

        assertEquals(listOf(2, 1), report.takes.map { it.slideIndex })
        assertEquals(listOf("slide-02.mp3", "slide-01.mp3"), report.takes.map { it.audioFile })
    }
}
