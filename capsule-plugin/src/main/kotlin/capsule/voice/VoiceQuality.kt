package capsule.voice

/**
 * Acceptance thresholds for voice-quality verification (CAP-QC-VOICE US-2).
 *
 * Ported from `prepa_electricite_complet/verif_voix.py` defaults: a take is
 * accepted when its word error rate stays at or below [maxWordErrorRate], its
 * surplus (extra transcribed words = hallucination) stays below
 * [surplusThreshold], and its background headroom (SNR) stays at or above
 * [minSnrDb]. The defaults are conservative — the QC is a *signal*, the pilot
 * decides.
 *
 * @param maxWordErrorRate  edit distance / reference word count ceiling (default 0.15).
 * @param minSnrDb          background-to-voice headroom floor in dB (default 66.0).
 * @param surplusThreshold  extra transcribed words that signal a hallucination (default 3).
 */
data class VoiceThresholds(
    val maxWordErrorRate: Double = 0.15,
    val minSnrDb: Double = 66.0,
    val surplusThreshold: Int = 3,
)

/**
 * Verdict of one synthesised take (CAP-QC-VOICE US-2).
 *
 * - [Pass] — every threshold holds.
 * - [Fail] — one or more thresholds were breached; [Fail.reasons] names them.
 * - [Degraded] — the take could not be verified (ASR absent or failed); this is
 *   **not** a failure, it is an explicit "not checked" signal.
 */
sealed interface VoiceVerdict {

    /** The take passed every threshold. */
    data object Pass : VoiceVerdict

    /**
     * One or more thresholds were breached.
     *
     * @param reasons stable machine-readable reason codes (e.g. `"word-error-rate"`,
     *        `"surplus"`, `"empty-transcription"`, `"snr"`).
     */
    data class Fail(val reasons: List<String>) : VoiceVerdict

    /**
     * The take could not be verified.
     *
     * @param reason why verification was skipped (e.g. `"asr-unavailable"`,
     *        `"asr-error"`).
     */
    data class Degraded(val reason: String) : VoiceVerdict

    /** `true` when this verdict is a [Fail]. */
    val isFail: Boolean get() = this is Fail
}

/**
 * Pure decision service for the voice-quality verdict (CAP-QC-VOICE US-2).
 *
 * Given a scored take ([VoiceScore]) and its optional SNR estimate, decides
 * whether it passes [VoiceThresholds]. No I/O, no ASR — the caller runs the
 * recogniser and the RMS extraction, then hands the numbers here.
 *
 * An **empty transcription** of a non-empty reference is reported as
 * `empty-transcription` (the NoOp placeholder signal, C2 of the S-169 audit)
 * in addition to the mechanical `word-error-rate` breach it implies.
 */
object VoiceQualityGate {

    /** Reason code: word error rate above the ceiling. */
    const val REASON_WORD_ERROR_RATE: String = "word-error-rate"

    /** Reason code: surplus (hallucinated words) above the threshold. */
    const val REASON_SURPLUS: String = "surplus"

    /** Reason code: background headroom (SNR) below the floor. */
    const val REASON_SNR: String = "snr"

    /** Reason code: the reference is non-empty but nothing was transcribed. */
    const val REASON_EMPTY_TRANSCRIPTION: String = "empty-transcription"

    /** Reason code: the take carries no audible signal (mute audio). */
    const val REASON_SILENT_AUDIO: String = "silent-audio"

    /**
     * Decides the verdict of a scored take.
     *
     * @param score      the fidelity score (word error rate + surplus).
     * @param snrDb      the estimated SNR in dB, or `null` when the envelope was
     *                   not available (the SNR check is then skipped).
     * @param thresholds the acceptance thresholds.
     * @param silence    the optional RMS-envelope silence analysis. `null` skips
     *                   the silence check (envelope unavailable).
     * @return [VoiceVerdict.Pass] when every available check holds,
     *         [VoiceVerdict.Fail] with the breached reason codes otherwise.
     */
    fun verdict(
        score: VoiceScore,
        snrDb: Double?,
        thresholds: VoiceThresholds,
        silence: SilenceAnalysis? = null,
    ): VoiceVerdict {
        val reasons = mutableListOf<String>()
        if (score.referenceWordCount > 0 && score.hypothesisWordCount == 0) {
            reasons += REASON_EMPTY_TRANSCRIPTION
        }
        if (score.wordErrorRate > thresholds.maxWordErrorRate) {
            reasons += REASON_WORD_ERROR_RATE
        }
        if (score.surplus >= thresholds.surplusThreshold) {
            reasons += REASON_SURPLUS
        }
        if (snrDb != null && snrDb < thresholds.minSnrDb) {
            reasons += REASON_SNR
        }
        if (silence?.isSilent == true) {
            reasons += REASON_SILENT_AUDIO
        }
        return if (reasons.isEmpty()) VoiceVerdict.Pass else VoiceVerdict.Fail(reasons)
    }
}
