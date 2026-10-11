package capsule.voice

/**
 * Estimates the background-noise headroom (SNR) of a take (CAP-QC-VOICE US-1).
 *
 * Ported from `prepa_electricite_complet/verif_voix.py` (`souffle`): the noise
 * floor is the quietest frame's RMS, the voice level is the RMS of every frame at
 * or above the 60th percentile, and the estimate is `20·log10(voice / floor)` in
 * dB. Below the tuned threshold (66 dB on the reference clone) the breath is
 * audible on headphones.
 *
 * Pure — no I/O. The caller extracts the per-frame RMS from the audio (an I/O
 * concern handled by the ASR adapter in a later US); this object only does the
 * arithmetic.
 */
object SnrEstimator {

    /** Floor applied under the ratio to avoid a logarithm of zero. */
    private const val FLOOR = 1e-9

    /**
     * @param frameRms per-frame RMS amplitudes (one entry per short frame).
     * @return the estimated SNR in dB, or 0.0 when [frameRms] is empty.
     */
    fun estimate(frameRms: DoubleArray): Double {
        if (frameRms.isEmpty()) return 0.0

        val noiseFloor = frameRms.min()
        val sorted = frameRms.sortedArray()
        val percentileIndex = ((sorted.size - 1) * VOICE_PERCENTILE).toInt().coerceIn(0, sorted.size - 1)
        val threshold = sorted[percentileIndex]

        var sumSquares = 0.0
        var count = 0
        for (rms in frameRms) {
            if (rms >= threshold) {
                sumSquares += rms * rms
                count++
            }
        }
        if (count == 0) return 0.0
        val voice = kotlin.math.sqrt(sumSquares / count)
        return 20.0 * kotlin.math.log10(voice / maxOf(noiseFloor, FLOOR))
    }

    /** Frames at or above this quantile form the "voice" level. */
    const val VOICE_PERCENTILE: Double = 0.6
}
