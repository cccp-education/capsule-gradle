package capsule.voice

/**
 * Fidelity of one synthesised take against its expected text (CAP-QC-VOICE US-1).
 *
 * @param wordErrorRate        edit distance / reference word count (>= 0).
 * @param distance             word-level edit distance.
 * @param referenceWordCount   number of words in the expected (normalised) text.
 * @param hypothesisWordCount  number of words in the (normalised) transcription.
 */
data class VoiceScore(
    val wordErrorRate: Double,
    val distance: Int,
    val referenceWordCount: Int,
    val hypothesisWordCount: Int,
) {
    /** Extra transcribed words (a hallucination signal) — may be negative. */
    val surplus: Int get() = hypothesisWordCount - referenceWordCount
}

/**
 * Scores a take's fidelity from the expected text and an ASR transcription
 * (CAP-QC-VOICE US-1).
 *
 * Ported from `prepa_electricite_complet/verif_voix.py`: both sides are
 * normalised ([TextNormalizer]) and aligned word by word ([WordAligner]); the
 * `wordErrorRate` is the distance divided by the reference word count, and the
 * `surplus` (extra words) is the hallucination signal.
 *
 * Pure — no I/O, no ASR: the transcription is given as a string.
 */
object FidelityScorer {

    /**
     * Scores the raw [expected] text against the raw [transcribed] text.
     *
     * PRT-02 residual: integer numerals are spelled ([NumberSpeller]) before
     * normalisation, so "il y a 42 pommes" and "il y a quarante deux pommes"
     * compare equal instead of producing a false difference.
     */
    fun score(expected: String, transcribed: String, language: String = "fr"): VoiceScore =
        score(
            TextNormalizer.normalize(NumberSpeller.spell(expected, language)),
            TextNormalizer.normalize(NumberSpeller.spell(transcribed, language)),
        )

    /** Scores already-normalised word lists. */
    fun score(referenceWords: List<String>, hypothesisWords: List<String>): VoiceScore {
        val alignment = WordAligner.align(referenceWords, hypothesisWords)
        val referenceCount = referenceWords.size
        val wordErrorRate = alignment.distance.toDouble() / maxOf(1, referenceCount)
        return VoiceScore(
            wordErrorRate = wordErrorRate,
            distance = alignment.distance,
            referenceWordCount = referenceCount,
            hypothesisWordCount = hypothesisWords.size,
        )
    }
}
