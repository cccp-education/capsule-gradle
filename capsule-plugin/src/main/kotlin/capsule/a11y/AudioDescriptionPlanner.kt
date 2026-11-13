package capsule.a11y

/**
 * One window of the narration track, in seconds (CAP-AUDIODESC US-1).
 *
 * Audio description is spoken **into the silences** of the narration: the
 * planner never overlaps a [NarrationWindow], so callers must describe where the
 * narration actually speaks.
 *
 * Invariants (fail-fast):
 * - [startSeconds] must be `>= 0`.
 * - [endSeconds] must be strictly greater than [startSeconds].
 */
data class NarrationWindow(
    val startSeconds: Double,
    val endSeconds: Double,
) {
    init {
        require(startSeconds >= 0.0) { "NarrationWindow.startSeconds must be >= 0 (was $startSeconds)" }
        require(endSeconds > startSeconds) {
            "NarrationWindow.endSeconds must be > startSeconds (was $endSeconds <= $startSeconds)"
        }
    }

    /** Length of the window in seconds. */
    val durationSeconds: Double get() = endSeconds - startSeconds
}

/**
 * One audio-description cue to place in a silence (CAP-AUDIODESC US-1).
 *
 * The cue carries the authored description and its **estimated spoken
 * duration** (the planner is pure: it does not synthesize audio and cannot
 * measure it).
 *
 * Invariants (fail-fast):
 * - [text] must not be blank.
 * - [durationSeconds] must be strictly positive.
 */
data class AudioDescriptionCue(
    val text: String,
    val durationSeconds: Double,
) {
    init {
        require(text.isNotBlank()) { "AudioDescriptionCue.text must not be blank" }
        require(durationSeconds > 0.0) {
            "AudioDescriptionCue.durationSeconds must be > 0 (was $durationSeconds)"
        }
    }
}

/**
 * One placed audio-description segment (CAP-AUDIODESC US-1).
 *
 * By construction a segment lies entirely inside a silence: it never overlaps
 * the narration.
 */
data class AudioDescriptionSegment(
    val text: String,
    val startSeconds: Double,
    val endSeconds: Double,
) {
    init {
        require(text.isNotBlank()) { "AudioDescriptionSegment.text must not be blank" }
        require(startSeconds >= 0.0) { "AudioDescriptionSegment.startSeconds must be >= 0" }
        require(endSeconds > startSeconds) { "AudioDescriptionSegment.endSeconds must be > startSeconds" }
    }

    /** Spoken length of the segment in seconds. */
    val durationSeconds: Double get() = endSeconds - startSeconds
}

/**
 * Planned audio-description track (CAP-AUDIODESC US-1).
 *
 * [unplaced] **declares the limit** when the narration leaves too little silence
 * for every cue: nothing is silently dropped, and nothing is squeezed over the
 * narration.
 */
data class AudioDescriptionScript(
    val segments: List<AudioDescriptionSegment>,
    val unplaced: List<AudioDescriptionCue> = emptyList(),
) {
    /** `true` when no segment was placed. */
    val isEmpty: Boolean get() = segments.isEmpty()

    /** `true` when at least one cue could not be placed in a silence. */
    val hasUnplaced: Boolean get() = unplaced.isNotEmpty()

    /** Total spoken length of the placed segments, in seconds. */
    val totalSpokenSeconds: Double get() = segments.sumOf { it.durationSeconds }
}

/**
 * Pure planner that places audio-description cues into the silences left by the
 * narration (CAP-AUDIODESC US-1).
 *
 * Determinism: narration windows are sorted and merged, silences are computed
 * from `0` to the last narration end (plus an optional trailing silence when a
 * total duration is provided), and cues are placed **first-fit in the given
 * order**. The result never overlaps the narration.
 *
 * No I/O, no TTS, no FFmpeg: US-3 consumes this plan to synthesize and mix the
 * description.
 */
object AudioDescriptionPlanner {
    /** Default minimum silence length accepted for placing a cue. */
    const val DEFAULT_MIN_GAP_SECONDS: Double = 0.5

    /**
     * Places [cues] into the silences of [narration].
     *
     * @param cues                authored descriptions, in the desired order.
     * @param narration           narration windows (any order, overlaps merged).
     * @param minGapSeconds       silences shorter than this are unusable.
     * @param totalDurationSeconds optional clip length; when provided, the silence
     *                             after the last narration window is usable too.
     */
    fun plan(
        cues: List<AudioDescriptionCue>,
        narration: List<NarrationWindow>,
        minGapSeconds: Double = DEFAULT_MIN_GAP_SECONDS,
        totalDurationSeconds: Double? = null,
    ): AudioDescriptionScript {
        require(minGapSeconds >= 0.0) { "minGapSeconds must be >= 0 (was $minGapSeconds)" }
        require(totalDurationSeconds == null || totalDurationSeconds >= 0.0) {
            "totalDurationSeconds must be >= 0 (was $totalDurationSeconds)"
        }

        val gaps = silences(narration, minGapSeconds, totalDurationSeconds)
        val remaining = gaps.map { it.first to it.second }.toMutableList()
        val placed = mutableListOf<AudioDescriptionSegment>()
        val unplaced = mutableListOf<AudioDescriptionCue>()

        for (cue in cues) {
            var wasPlaced = false
            for (index in remaining.indices) {
                val (start, end) = remaining[index]
                if (end - start >= cue.durationSeconds) {
                    val cueEnd = start + cue.durationSeconds
                    placed.add(
                        AudioDescriptionSegment(
                            text = cue.text,
                            startSeconds = start,
                            endSeconds = cueEnd,
                        ),
                    )
                    remaining[index] = cueEnd to end
                    wasPlaced = true
                    break
                }
            }
            if (!wasPlaced) unplaced.add(cue)
        }

        return AudioDescriptionScript(segments = placed, unplaced = unplaced)
    }

    /** Usable silences `[start, end]`, in ascending order. */
    private fun silences(
        narration: List<NarrationWindow>,
        minGapSeconds: Double,
        totalDurationSeconds: Double?,
    ): List<Pair<Double, Double>> {
        val merged = merge(narration)
        val gaps = mutableListOf<Pair<Double, Double>>()
        var cursor = 0.0
        for (window in merged) {
            if (window.startSeconds - cursor >= minGapSeconds) {
                gaps.add(cursor to window.startSeconds)
            }
            cursor = maxOf(cursor, window.endSeconds)
        }
        if (totalDurationSeconds != null && totalDurationSeconds - cursor >= minGapSeconds) {
            gaps.add(cursor to totalDurationSeconds)
        }
        return gaps
    }

    /** Sorts windows and merges every overlap or adjacency. */
    private fun merge(narration: List<NarrationWindow>): List<NarrationWindow> {
        val sorted = narration.sortedBy { it.startSeconds }
        val merged = mutableListOf<NarrationWindow>()
        for (window in sorted) {
            val last = merged.lastOrNull()
            if (last != null && window.startSeconds <= last.endSeconds) {
                merged[merged.lastIndex] =
                    NarrationWindow(startSeconds = last.startSeconds, endSeconds = maxOf(last.endSeconds, window.endSeconds))
            } else {
                merged.add(window)
            }
        }
        return merged
    }
}
