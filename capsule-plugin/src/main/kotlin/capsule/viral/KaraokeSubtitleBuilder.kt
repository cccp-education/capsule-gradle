package capsule.viral

/**
 * One word with its time span, for karaoke (word-by-word) styling
 * (CAP-VIRAL US-12).
 *
 * @property text       the word (must not be blank).
 * @property startTime  start time in seconds (>= 0).
 * @property endTime    end time in seconds (>= [startTime]).
 */
data class KaraokeWord(
    val text: String,
    val startTime: Double,
    val endTime: Double,
) {
    init {
        require(text.isNotBlank()) { "KaraokeWord.text must not be blank" }
        require(startTime >= 0.0) { "KaraokeWord.startTime must be >= 0, got $startTime" }
        require(endTime >= startTime) { "KaraokeWord.endTime ($endTime) must be >= startTime ($startTime)" }
    }

    /** Duration in seconds. */
    val durationSeconds: Double get() = endTime - startTime
}

/**
 * Pure builder of karaoke (word-by-word) ASS dialogue text (CAP-VIRAL US-12).
 *
 * The short-form viral burn-in uses the ASS `\k` karaoke override so each word
 * is highlighted as it is spoken — the retention-critical caption style for
 * TikTok/Shorts/Reels (distinct from the pedagogical plain burn-in of
 * `SubtitleBurnInStyle`).
 *
 * `\k<duration-in-centiseconds>` advances the karaoke highlight; the builder
 * emits one `\k` per word. Times are formatted as ASS `H:MM:SS.cc`.
 *
 * Pure — no I/O. Pattern: `capsule.SubtitleBurnInCommand` (pure command/text
 * builder).
 */
object KaraokeSubtitleBuilder {

    /**
     * Builds the ASS karaoke dialogue text for [words].
     *
     * @param words ordered words with their time spans.
     * @return the karaoke text (`{\k..}word{\k..}word...`), or blank when
     *         [words] is empty (degraded — no karaoke line is emitted).
     */
    fun buildKaraokeText(words: List<KaraokeWord>): String {
        if (words.isEmpty()) return ""
        return words.joinToString(separator = " ") { word ->
            val centiseconds = (word.durationSeconds * 100).toInt().coerceAtLeast(1)
            "{\\k$centiseconds}${word.text}"
        }
    }

    /**
     * Formats [seconds] as an ASS timestamp `H:MM:SS.cc`.
     *
     * @param seconds time in seconds (>= 0).
     * @return the ASS timestamp string.
     */
    fun formatAssTimestamp(seconds: Double): String {
        val totalCentis = (seconds * 100).toLong().coerceAtLeast(0)
        val centis = totalCentis % 100
        val totalSecs = totalCentis / 100
        val s = totalSecs % 60
        val totalMins = totalSecs / 60
        val m = totalMins % 60
        val h = totalMins / 60
        return "%d:%02d:%02d.%02d".format(h, m, s, centis)
    }
}
