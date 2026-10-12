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
     * Plans the karaoke timeline for [beats], timing words proportionally to
     * their length within each beat window (the default, ASR-free behaviour).
     *
     * @param beats the storyboard beats, in order (intents are the spoken text).
     * @return one [KaraokeLine] per beat, in order, each spanning the beat window.
     */
    fun plan(beats: List<StoryboardBeat>): List<KaraokeLine> = plan(beats, emptyList())

    /**
     * Plans the karaoke timeline for [beats], using real ASR word [timings] where
     * they match the beat words (CAP-QC-VOICE US-5) and falling back to the
     * proportional timing otherwise.
     *
     * A beat uses the measured timings only when **every** word of its intent is
     * matched, in order, from the current cursor; the matched spans are clamped to
     * the beat window so a small ASR drift can never push a highlight outside its
     * line. This keeps the karaoke deterministic and never invents a timing.
     *
     * @param timings ASR word timings ([WordTimingsJson.parse]); empty → proportional.
     */
    fun plan(beats: List<StoryboardBeat>, timings: List<TimedWord>): List<KaraokeLine> {
        var cursor = 0.0
        var timingCursor = 0
        return beats.map { beat ->
            val start = cursor
            val end = start + beat.durationSeconds
            cursor = end
            val words = splitWords(beat.intent)

            val measured = if (timings.isEmpty()) null else matchTimings(words, timings, timingCursor)
            val wordsOrNull: List<KaraokeWord>? = measured?.let { (ranges, nextCursor) ->
                timingCursor = nextCursor
                ranges.mapIndexed { index, range ->
                    val from = range.start.coerceIn(start, end)
                    val to = range.endInclusive.coerceIn(from, end)
                    KaraokeWord(words[index], from, to)
                }
            }
            KaraokeLine(
                words = wordsOrNull ?: distribute(words, start, end),
                startTime = start,
                endTime = end,
            )
        }
    }

    /**
     * Matches every [word] to a [TimedWord] from [from] onward (order-preserving).
     *
     * @return the matched spans (clamped by the caller) and the cursor past the
     *         last match, or `null` when any word is unmatched (the beat then falls
     *         back to proportional timing).
     */
    private fun matchTimings(
        words: List<String>,
        timings: List<TimedWord>,
        from: Int,
    ): Pair<List<ClosedFloatingPointRange<Double>>, Int>? {
        val ranges = ArrayList<ClosedFloatingPointRange<Double>>(words.size)
        var cursor = from
        for (word in words) {
            val target = WordTimingsJson.normalize(word)
            val index = (cursor until timings.size).firstOrNull {
                WordTimingsJson.normalize(timings[it].text) == target
            } ?: return null
            ranges.add(timings[index].start..timings[index].end)
            cursor = index + 1
        }
        return ranges to cursor
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
