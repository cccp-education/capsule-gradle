package capsule.timeline

/**
 * Deterministic timeline for a capsule (PRT-07).
 *
 * Ported from `prepa_electricite_complet/montage_audio.py`: every scene lasts an
 * INTEGER number of frames, so sound and image cannot drift, even on an hour.
 * Capsule is one audio take per slide, so the model is **voice-anchored and
 * contiguous**: each slide occupies the frames of its own audio (at least one),
 * with an optional `lead` before the first slide, `tail` after the last, and
 * `avance` (the visual leads the voice within a slide — prepa's `AVANCE`).
 *
 * Pure — no I/O.
 */

/**
 * @param fps          frames per second (must be > 0).
 * @param leadSeconds  silence before the first slide.
 * @param tailSeconds  silence after the last slide.
 * @param avanceSeconds how far the visual leads the voice within a slide
 *        (applied to every slide but the first, whose voice starts the timeline).
 */
data class TimelineConfig(
    val fps: Int = 30,
    val leadSeconds: Double = 0.0,
    val tailSeconds: Double = 0.0,
    val avanceSeconds: Double = 0.0,
) {
    init {
        require(fps > 0) { "fps must be > 0, got $fps" }
        require(leadSeconds >= 0.0) { "leadSeconds must be >= 0, got $leadSeconds" }
        require(tailSeconds >= 0.0) { "tailSeconds must be >= 0, got $tailSeconds" }
        require(avanceSeconds >= 0.0) { "avanceSeconds must be >= 0, got $avanceSeconds" }
    }
}

/**
 * One slide on the timeline.
 *
 * @param slideIndex      0-based position.
 * @param startFrame      first frame the slide is displayed.
 * @param frameCount      integer number of frames the slide is displayed (>= 1).
 * @param voiceStartFrame frame at which the slide's voice begins (>= [startFrame]).
 */
data class TimelineEntry(
    val slideIndex: Int,
    val startFrame: Long,
    val frameCount: Long,
    val voiceStartFrame: Long,
) {
    init {
        require(frameCount >= 1) { "frameCount must be >= 1, got $frameCount" }
        require(voiceStartFrame >= startFrame) { "voiceStartFrame must be >= startFrame" }
    }

    /** First frame after this slide. */
    val endFrame: Long get() = startFrame + frameCount
}

/** A full capsule timeline at a fixed [fps]. */
data class Timeline(val fps: Int, val entries: List<TimelineEntry>) {
    val totalFrames: Long get() = entries.lastOrNull()?.endFrame ?: 0L
    val totalSeconds: Double get() = if (fps > 0) totalFrames.toDouble() / fps else 0.0
}

/**
 * Pure builder of a [Timeline] from per-slide audio durations.
 */
object TimelineBuilder {

    /** Rounds [seconds] to whole frames at [fps], never below zero. */
    fun framesFor(seconds: Double, fps: Int): Long =
        kotlin.math.max(0L, kotlin.math.round(seconds * fps).toLong())

    /** Converts [frames] at [fps] back to an exact duration in seconds. */
    fun secondsFor(frames: Long, fps: Int): Double =
        if (fps > 0) frames.toDouble() / fps else 0.0

    /**
     * Builds the timeline for [durations] (one audio duration per slide).
     *
     * The slides are contiguous: each occupies its audio frames (at least one),
     * the first starts after the `lead`, the last also carries the `tail`, and a
     * non-first slide's voice starts `avance` frames after its visual start.
     */
    fun build(durations: List<Double>, config: TimelineConfig = TimelineConfig()): Timeline {
        val fps = config.fps
        val leadFrames = framesFor(config.leadSeconds, fps)
        val tailFrames = framesFor(config.tailSeconds, fps)
        val avanceFrames = framesFor(config.avanceSeconds, fps)

        val entries = ArrayList<TimelineEntry>(durations.size)
        var cursor = leadFrames
        for ((index, duration) in durations.withIndex()) {
            val audioFrames = kotlin.math.max(1L, framesFor(duration, fps))
            val isLast = index == durations.lastIndex
            val frameCount = if (isLast) audioFrames + tailFrames else audioFrames
            val voiceStartFrame = cursor + if (index > 0) avanceFrames else 0L
            entries.add(TimelineEntry(index, cursor, frameCount, voiceStartFrame))
            cursor += audioFrames
        }
        return Timeline(fps, entries)
    }
}
