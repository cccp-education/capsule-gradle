package capsule

/**
 * Splits a subtitle line into "cartons" (cues) short enough to read (PRT-03).
 *
 * Ported from `prepa_electricite_complet/subs.py:cartons`: wrap on word
 * boundaries at [maxChars], and additionally break after end-of-sentence
 * punctuation once the line is long enough. A word is never split, and no empty
 * carton is ever emitted.
 *
 * Pure — no I/O.
 */
object SubtitleCartoner {

    /** Default maximum characters per cue (reference value). */
    const val DEFAULT_MAX_CHARS: Int = 42

    /** Minimum length before a sentence break is honoured (avoids 1-word cues). */
    private const val MIN_SENTENCE_BREAK: Int = 18

    private val SENTENCE_END = setOf('.', '?', '!', ':')

    private val WHITESPACE = Regex("""\s+""")

    /**
     * Splits [text] into cartons of at most [maxChars] characters.
     *
     * @param maxChars the maximum length of a carton (must be > 0).
     * @return the cartons in order; empty when [text] has no words.
     */
    fun split(text: String, maxChars: Int = DEFAULT_MAX_CHARS): List<String> {
        require(maxChars > 0) { "maxChars must be > 0, got $maxChars" }
        val words = text.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val cartons = mutableListOf<String>()
        val current = StringBuilder()
        for (word in words) {
            if (current.isNotEmpty() && current.length + 1 + word.length > maxChars) {
                cartons.add(current.toString())
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
            if (word.last() in SENTENCE_END && current.length > MIN_SENTENCE_BREAK) {
                cartons.add(current.toString())
                current.clear()
            }
        }
        if (current.isNotEmpty()) cartons.add(current.toString())
        return cartons
    }
}
