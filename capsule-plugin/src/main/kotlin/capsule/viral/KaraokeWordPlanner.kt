package capsule.viral

/**
 * One karaoke subtitle line (CAP-VIRAL-E2E US-3): the words of a storyboard beat,
 * spanned over the beat's [startTime]–[endTime] window.
 *
 * @property words     ordered words with their time spans (may be empty).
 * @property startTime line start in seconds (>= 0).
 * @property endTime   line end in seconds (>= [startTime]).
 */
data class KaraokeLine(
    val words: List<KaraokeWord>,
    val startTime: Double,
    val endTime: Double,
) {
    init {
        require(startTime >= 0.0) { "KaraokeLine.startTime must be >= 0, got $startTime" }
        require(endTime >= startTime) { "KaraokeLine.endTime ($endTime) must be >= startTime ($startTime)" }
    }

    /** Duration in seconds. */
    val durationSeconds: Double get() = endTime - startTime
}

/**
 * Pure planner of the karaoke timeline (CAP-VIRAL-E2E US-3).
 *
 * The viral video is short and beat-driven: each [StoryboardBeat] is one spoken
 * line. This object turns the beats into [KaraokeLine]s — one per beat, words
 * timed **proportionally to their length** within the beat window — so the
 * retention-critical word-by-word highlight tracks the voice deterministically
 * (no ASR, no timing guess: the beat durations already drive the capture).
 *
 * Pure — no I/O, no LLM. The spoken text is the beat intent (never invented).
 */
object KaraokeWordPlanner {

    /**
     * Plans the karaoke timeline for [beats].
     *
     * @param beats the storyboard beats, in order (intents are the spoken text).
     * @return one [KaraokeLine] per beat, in order, each spanning the beat window.
     */
    fun plan(beats: List<StoryboardBeat>): List<KaraokeLine> {
        var cursor = 0.0
        return beats.map { beat ->
            val start = cursor
            val end = start + beat.durationSeconds
            cursor = end
            val words = splitWords(beat.intent)
            KaraokeLine(words = distribute(words, start, end), startTime = start, endTime = end)
        }
    }

    private fun splitWords(text: String): List<String> =
        text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    /** Distributes [words] over [start]–[end], each word proportional to its length. */
    private fun distribute(words: List<String>, start: Double, end: Double): List<KaraokeWord> {
        if (words.isEmpty()) return emptyList()
        val total = words.sumOf { it.length }.toDouble()
        val span = end - start
        var t = start
        return words.map { word ->
            val duration = span * (word.length / total)
            val entry = KaraokeWord(word, t, t + duration)
            t += duration
            entry
        }
    }
}
