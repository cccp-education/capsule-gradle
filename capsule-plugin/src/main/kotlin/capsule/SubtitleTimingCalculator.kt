package capsule

import capsule.feed.SlideSegment

object SubtitleTimingCalculator {

    fun calculate(slides: List<SlideSegment>, durations: List<Double>): List<SubtitleEntry> {
        if (slides.isEmpty()) return emptyList()

        val entries = mutableListOf<SubtitleEntry>()
        var currentTime = 0.0

        for (i in slides.indices) {
            val slide = slides[i]
            val duration = durations.getOrElse(i) { 0.0 }
            val endTime = currentTime + duration

            entries.add(
                SubtitleEntry(
                    index = slide.index,
                    startTime = currentTime,
                    endTime = endTime,
                    text = slide.speakerNote
                )
            )

            currentTime = endTime
        }

        return entries
    }

    /**
     * PRT-03 — one cue per carton.
     *
     * Each slide's speaker note is normalised for display, split into readable
     * cartons, and the slide duration is distributed across them proportionally
     * to their length. Cue indices are renumbered sequentially across the whole
     * deck (1-based), so a 3-carton slide followed by a 2-carton slide yields
     * cues 1..5.
     *
     * @param maxChars maximum characters per carton.
     * @param reversals pronunciation→display substitutions (see
     *        [SubtitleTextNormalizer]).
     */
    fun calculateFromCartons(
        slides: List<SlideSegment>,
        durations: List<Double>,
        maxChars: Int = SubtitleCartoner.DEFAULT_MAX_CHARS,
        reversals: Map<String, String> = emptyMap(),
    ): List<SubtitleEntry> {
        if (slides.isEmpty()) return emptyList()

        val entries = mutableListOf<SubtitleEntry>()
        var currentTime = 0.0
        var cueIndex = 1

        for (i in slides.indices) {
            val slide = slides[i]
            val duration = durations.getOrElse(i) { 0.0 }
            val display = SubtitleTextNormalizer.spokenToDisplay(slide.speakerNote, reversals)
            val cartons = SubtitleCartoner.split(display, maxChars)

            if (cartons.isNotEmpty()) {
                val ranges = SubtitleTimingDistributor.distribute(cartons, currentTime, duration)
                cartons.forEachIndexed { k, carton ->
                    val range = ranges[k]
                    entries.add(
                        SubtitleEntry(
                            index = cueIndex,
                            startTime = range.start,
                            endTime = range.endInclusive,
                            text = carton,
                        )
                    )
                    cueIndex++
                }
            }

            currentTime += duration
        }

        return entries
    }
}
