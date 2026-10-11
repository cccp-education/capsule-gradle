package capsule

/**
 * Distributes a take's duration across its cartons, proportionally to their
 * length (PRT-03).
 *
 * Ported from `prepa_electricite_complet/subs.py`: each carton owns the fraction
 * of the take equal to its character share, so a long cue stays on screen longer
 * than a short one. The ranges are contiguous and, together, cover
 * `[start, start + duration]` exactly in order — no gap, no overlap.
 *
 * Pure — no I/O.
 */
object SubtitleTimingDistributor {

    /**
     * @param cartons  the cartons in order (each non-empty).
     * @param start    the take's start time in seconds (>= 0).
     * @param duration the take's duration in seconds (>= 0).
     * @return one closed range per carton, contiguous and covering the take.
     */
    fun distribute(cartons: List<String>, start: Double, duration: Double): List<ClosedRange<Double>> {
        require(start >= 0.0) { "start must be >= 0, got $start" }
        require(duration >= 0.0) { "duration must be >= 0, got $duration" }
        if (cartons.isEmpty()) return emptyList()

        val total = cartons.sumOf { it.length }.coerceAtLeast(1)
        val ranges = ArrayList<ClosedRange<Double>>(cartons.size)
        var consumed = 0
        for (carton in cartons) {
            val from = start + duration * consumed / total
            consumed += carton.length
            val to = start + duration * consumed / total
            ranges.add(from..to)
        }
        return ranges
    }
}
