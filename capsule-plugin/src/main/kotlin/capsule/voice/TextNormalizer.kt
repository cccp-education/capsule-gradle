package capsule.voice

import java.text.Normalizer

/**
 * Normalises text for voice-quality comparison (CAP-QC-VOICE US-1).
 *
 * Ported from `prepa_electricite_complet/verif_voix.py` (`norm`): lower-case,
 * fold `œ`/`æ`, strip diacritics, turn apostrophes and hyphens into spaces,
 * drop every non `[a-z0-9 ]` character, then split on whitespace. This is the
 * form under which an expected narration and an ASR transcription are compared,
 * so that accents, case and punctuation never produce a false difference.
 *
 * Pure — no I/O.
 */
object TextNormalizer {

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9 ]")
    private val WHITESPACE = Regex("\\s+")

    /** Returns the comparison words of [text] (empty when nothing survives). */
    fun normalize(text: String): List<String> {
        var s = text.lowercase()
            .replace("œ", "oe")
            .replace("æ", "ae")
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
        s = DIACRITICS.replace(s, "")
        s = s.replace("'", " ").replace("-", " ")
        s = NON_ALPHANUMERIC.replace(s, " ")
        return s.split(WHITESPACE).filter { it.isNotEmpty() }
    }
}
