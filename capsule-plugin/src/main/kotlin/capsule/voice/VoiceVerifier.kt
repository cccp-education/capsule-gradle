package capsule.voice

import java.io.File

/**
 * One take to verify: the expected narration, the audio that was synthesised
 * for it, and where it belongs (CAP-QC-VOICE US-2).
 *
 * @param deckName     deck the slide belongs to.
 * @param slideIndex   1-based slide position inside the deck.
 * @param expectedText the narration that was meant to be spoken.
 * @param audioFile    the synthesised `slide-NN.mp3` to transcribe.
 */
data class VoiceTake(
    val deckName: String,
    val slideIndex: Int,
    val expectedText: String,
    val audioFile: File,
)

/**
 * The verification outcome of one [VoiceTake] (CAP-QC-VOICE US-2).
 *
 * @param transcript    the ASR transcription (empty when degraded).
 * @param wordErrorRate the scored word error rate, or `null` when degraded.
 * @param surplus       extra transcribed words, or `null` when degraded.
 * @param snrDb         background headroom in dB, or `null` when not measured.
 * @param verdict       the take verdict ([VoiceVerdict]).
 */
data class TakeVerification(
    val deckName: String,
    val slideIndex: Int,
    val audioFile: String,
    val expectedText: String,
    val transcript: String,
    val wordErrorRate: Double?,
    val surplus: Int?,
    val snrDb: Double?,
    val verdict: VoiceVerdict,
)

/**
 * The aggregate voice-quality report for a capsule (CAP-QC-VOICE US-2).
 *
 * @param degraded `true` when the ASR half was unavailable, so nothing was
 *        actually verified.
 * @param takes    per-slide verifications, in input order.
 * @param thresholds the thresholds the report was produced against.
 */
data class VoiceReport(
    val degraded: Boolean,
    val takes: List<TakeVerification>,
    val thresholds: VoiceThresholds,
) {
    /** The takes that breached a threshold. */
    val failures: List<TakeVerification> get() = takes.filter { it.verdict.isFail }

    /** `true` when at least one take failed a threshold. */
    val hasFailure: Boolean get() = failures.isNotEmpty()
}

/**
 * Pure voice-quality orchestration (CAP-QC-VOICE US-2).
 *
 * The ASR half ([SpeechRecognizer]) and the audio-envelope half
 * (`envelopeProvider`) are **injected** — this object contains no I/O of its
 * own, so the whole decision path is testable with fakes and a real ASR is
 * never a prerequisite of `check`.
 *
 * A per-take recognition error degrades **that take only** (`asr-error`); the
 * caller decides whether an ASR-less run is a degraded report
 * ([degraded]) or, under `strictMode`, a build failure.
 */
object VoiceVerifier {

    /** Whole-report reason: the ASR binary is not available. */
    const val REASON_ASR_UNAVAILABLE: String = "asr-unavailable"

    /** Per-take reason: recognition failed on this audio. */
    const val REASON_ASR_ERROR: String = "asr-error"

    /**
     * Verifies every [take] against [thresholds].
     *
     * @param takes            the takes to verify.
     * @param recognizer       the ASR port (fake in tests).
     * @param thresholds       acceptance thresholds.
     * @param language         ASR language hint.
     * @param envelopeProvider per-frame RMS provider, or a provider returning
     *        `null` when the envelope cannot be extracted (SNR then skipped).
     */
    fun verify(
        takes: List<VoiceTake>,
        recognizer: SpeechRecognizer,
        thresholds: VoiceThresholds,
        language: String,
        envelopeProvider: (File) -> DoubleArray? = { null },
    ): VoiceReport {
        val results = takes.map { take ->
            try {
                val transcript = recognizer.transcribe(take.audioFile, language)
                val score = FidelityScorer.score(take.expectedText, transcript)
                val snr = envelopeProvider(take.audioFile)?.let { SnrEstimator.estimate(it) }
                TakeVerification(
                    deckName = take.deckName,
                    slideIndex = take.slideIndex,
                    audioFile = take.audioFile.name,
                    expectedText = take.expectedText,
                    transcript = transcript,
                    wordErrorRate = score.wordErrorRate,
                    surplus = score.surplus,
                    snrDb = snr,
                    verdict = VoiceQualityGate.verdict(score, snr, thresholds),
                )
            } catch (e: Exception) {
                TakeVerification(
                    deckName = take.deckName,
                    slideIndex = take.slideIndex,
                    audioFile = take.audioFile.name,
                    expectedText = take.expectedText,
                    transcript = "",
                    wordErrorRate = null,
                    surplus = null,
                    snrDb = null,
                    verdict = VoiceVerdict.Degraded(REASON_ASR_ERROR),
                )
            }
        }
        return VoiceReport(degraded = false, takes = results, thresholds = thresholds)
    }

    /**
     * Builds the report of a run where the ASR is unavailable: every take is
     * marked `Degraded` and **no** take is a failure — the QC is a signal, not
     * a prerequisite (an explicit `strictMode` failure is the caller's call).
     */
    fun degraded(
        takes: List<VoiceTake>,
        thresholds: VoiceThresholds,
        reason: String = REASON_ASR_UNAVAILABLE,
    ): VoiceReport {
        val results = takes.map { take ->
            TakeVerification(
                deckName = take.deckName,
                slideIndex = take.slideIndex,
                audioFile = take.audioFile.name,
                expectedText = take.expectedText,
                transcript = "",
                wordErrorRate = null,
                surplus = null,
                snrDb = null,
                verdict = VoiceVerdict.Degraded(reason),
            )
        }
        return VoiceReport(degraded = true, takes = results, thresholds = thresholds)
    }
}
