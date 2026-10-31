package capsule.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for [VoiceQualityGate] (CAP-QC-VOICE US-2). Pure — no I/O, no ASR.
 */
class VoiceQualityGateTest {

    private val thresholds = VoiceThresholds(maxWordErrorRate = 0.15, minSnrDb = 66.0, surplusThreshold = 3)

    private fun score(expected: String, transcribed: String): VoiceScore =
        FidelityScorer.score(expected, transcribed)

    @Test
    fun `a faithful take with a clean background passes`() {
        val verdict = VoiceQualityGate.verdict(score("bonjour le monde", "bonjour le monde"), 70.0, thresholds)
        assertEquals(VoiceVerdict.Pass, verdict)
    }

    @Test
    fun `a word error rate above the ceiling fails`() {
        val verdict = VoiceQualityGate.verdict(score("le chat noir", "le chien blanc"), 70.0, thresholds)
        assertTrue(verdict is VoiceVerdict.Fail)
        assertTrue(VoiceQualityGate.REASON_WORD_ERROR_RATE in verdict.reasons)
    }

    @Test
    fun `a surplus at the threshold fails`() {
        val verdict = VoiceQualityGate.verdict(score("a b c", "a b c d e f"), 70.0, thresholds)
        assertTrue(verdict is VoiceVerdict.Fail)
        assertTrue(VoiceQualityGate.REASON_SURPLUS in verdict.reasons)
    }

    @Test
    fun `an audible background fails the snr check`() {
        val verdict = VoiceQualityGate.verdict(score("bonjour", "bonjour"), 40.0, thresholds)
        assertTrue(verdict is VoiceVerdict.Fail)
        assertTrue(VoiceQualityGate.REASON_SNR in verdict.reasons)
    }

    @Test
    fun `a missing snr estimate skips the snr check`() {
        val verdict = VoiceQualityGate.verdict(score("bonjour le monde", "bonjour le monde"), null, thresholds)
        assertEquals(VoiceVerdict.Pass, verdict)
    }

    @Test
    fun `an empty transcription is an explicit placeholder signal`() {
        val verdict = VoiceQualityGate.verdict(score("bonjour le monde", ""), 70.0, thresholds)
        assertTrue(verdict is VoiceVerdict.Fail)
        assertTrue(VoiceQualityGate.REASON_EMPTY_TRANSCRIPTION in verdict.reasons)
    }

    @Test
    fun `a silent envelope fails with a silent-audio reason`() {
        val verdict = VoiceQualityGate.verdict(
            score("bonjour", "bonjour"),
            70.0,
            thresholds,
            silence = AudioSilenceDetector.analyze(DoubleArray(20) { 0.0 }),
        )

        assertTrue(verdict is VoiceVerdict.Fail)
        assertTrue(VoiceQualityGate.REASON_SILENT_AUDIO in verdict.reasons)
    }

    @Test
    fun `a missing silence analysis skips the silence check`() {
        val verdict = VoiceQualityGate.verdict(score("bonjour le monde", "bonjour le monde"), 70.0, thresholds)

        assertEquals(VoiceVerdict.Pass, verdict)
    }

    @Test
    fun `a fail collects every breached reason`() {
        val verdict = VoiceQualityGate.verdict(score("a b c", "x y z w v u"), 20.0, thresholds)
        assertTrue(verdict is VoiceVerdict.Fail)
        assertEquals(
            listOf(
                VoiceQualityGate.REASON_WORD_ERROR_RATE,
                VoiceQualityGate.REASON_SURPLUS,
                VoiceQualityGate.REASON_SNR,
            ),
            verdict.reasons,
        )
    }
}
