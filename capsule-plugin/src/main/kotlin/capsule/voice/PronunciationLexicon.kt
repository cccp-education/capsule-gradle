package capsule.voice

import java.security.MessageDigest

/**
 * "Dit / lu" pronunciation lexicon (PRT-01).
 *
 * The text a voice must **say** is not always the text on screen: symbols are
 * read aloud ("%" → "pour cent") and acronyms are spelled ("API" → "A P I").
 * Ported from `prepa_electricite_complet/subs.py` (`lisible`/reversals): the
 * voice gets the spoken form, the subtitle keeps the displayed form.
 *
 * Pure — no I/O, no Gradle.
 */
object PronunciationLexicon {

    /** Symbol → spoken word, applied to the voice track. */
    private val SYMBOLS: Map<String, String> = linkedMapOf(
        "%" to "pour cent",
        "&" to "et",
        "€" to "euros",
        "\$" to "dollars",
        "°" to "degres",
        "≈" to "environ",
        "→" to "vers",
    )

    /** A run of 2 to 5 uppercase letters, spelled for the voice (`API` → `A P I`). */
    private val ACRONYM = Regex("\\b[A-Z]{2,5}\\b")

    private val WHITESPACE = Regex("\\s+")

    /**
     * Returns the **spoken** form of [display]: symbols expanded and acronyms
     * spelled, whitespace collapsed. When nothing applies, the input is returned
     * trimmed (idempotent on already-spoken text without acronyms).
     */
    fun spoken(display: String): String {
        var out = display
        for ((symbol, word) in SYMBOLS) {
            out = out.replace(symbol, " $word ")
        }
        out = ACRONYM.replace(out) { match -> match.value.toCharArray().joinToString(" ") }
        return WHITESPACE.replace(out, " ").trim()
    }

    /**
     * The display-side substitutions (spoken word → symbol) consumed by
     * `SubtitleTextNormalizer.spokenToDisplay`, so a pronunciation spelling is
     * never shown verbatim. Spelled acronyms are re-collected there already.
     */
    fun reversals(): Map<String, String> =
        SYMBOLS.entries.associate { (symbol, word) -> word to symbol }

    /**
     * A short, stable content key of [spoken] (first 8 hex chars of its SHA-256).
     * Used to index the TTS cache on the **spoken** text: editing a pronunciation
     * invalidates only the affected takes.
     */
    fun cacheKey(spoken: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(spoken.toByteArray(Charsets.UTF_8))
        return digest.take(4).joinToString("") { byte -> "%02x".format(byte) }
    }
}
