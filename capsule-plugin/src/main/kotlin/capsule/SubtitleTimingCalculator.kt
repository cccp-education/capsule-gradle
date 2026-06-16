package capsule

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
}
