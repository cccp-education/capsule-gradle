package capsule.voice

/**
 * Result of an RMS-envelope silence analysis (CAP-AUDIO-SILENCE US-1).
 *
 * A training capsule whose take is silent is unusable — a NoOp TTS, a broken
 * audio export or a wrong file all produce a technically valid but mute MP3.
 * The analysis is deliberately independent of the ASR: a silent take must be
 * caught even when the speech recogniser is unavailable.
 *
 * @param totalFrames  number of full RMS frames analysed (0 = empty audio).
 * @param silentFrames frames whose RMS is at or below [threshold].
 * @param peakRms      loudest frame in the take.
 * @param threshold    normalised RMS at or below which a frame is silent.
 */
data class SilenceAnalysis(
    val totalFrames: Int,
    val silentFrames: Int,
    val peakRms: Double,
    val threshold: Double,
) {
    init {
        require(totalFrames >= 0) { "totalFrames must be >= 0, got $totalFrames" }
        require(silentFrames in 0..totalFrames) {
            "silentFrames must be in 0..$totalFrames, got $silentFrames"
        }
        require(peakRms.isFinite() && peakRms >= 0.0) {
            "peakRms must be a finite non-negative value, got $peakRms"
        }
        require(threshold.isFinite() && threshold >= 0.0) {
            "threshold must be a finite non-negative value, got $threshold"
        }
    }

    /** Fraction of frames at or below [threshold] (`1.0` for empty audio). */
    val silentFraction: Double
        get() = if (totalFrames == 0) 1.0 else silentFrames.toDouble() / totalFrames

    /**
     * `true` when the take carries no audible signal at all: empty audio or a
     * peak at/below the silence threshold.
     *
     * The decision uses the **peak**, not the silent fraction: normal narration
     * contains pauses (silent frames), but a take with any audible frame is not
     * digital silence. [silentFraction] stays available for diagnostics.
     */
    val isSilent: Boolean
        get() = totalFrames == 0 || peakRms <= threshold
}

/**
 * Pure silence detector over the per-frame RMS envelope (CAP-AUDIO-SILENCE US-1).
 *
 * Input is the envelope produced by [RmsEnvelope] (or any equivalent frame
 * reader). No I/O, no ASR, no ffmpeg: the caller decodes the take and hands the
 * frames here, so the whole decision is unit-testable and can run even when the
 * ASR engine is absent.
 */
object AudioSilenceDetector {

    /**
     * Normalised RMS threshold (default `0.001`, about -60 dBFS).
     *
     * TTS speech frames are typically two orders of magnitude above it, while
     * digital silence is exactly `0.0`; the threshold therefore catches mute
     * takes without flagging quiet narration.
     */
    const val DEFAULT_RMS_THRESHOLD: Double = 0.001

    /**
     * Analyses [frames].
     *
     * @param frames    normalised RMS amplitudes (one per frame).
     * @param threshold normalised RMS at or below which a frame is silent.
     * @return the silence analysis (never throws for a finite envelope).
     */
    fun analyze(
        frames: DoubleArray,
        threshold: Double = DEFAULT_RMS_THRESHOLD,
    ): SilenceAnalysis {
        require(threshold.isFinite() && threshold >= 0.0) {
            "threshold must be a finite non-negative value, got $threshold"
        }
        var silentFrames = 0
        var peakRms = 0.0
        for (frame in frames) {
            if (frame <= threshold) silentFrames++
            if (frame > peakRms) peakRms = frame
        }
        return SilenceAnalysis(
            totalFrames = frames.size,
            silentFrames = silentFrames,
            peakRms = peakRms,
            threshold = threshold,
        )
    }
}
