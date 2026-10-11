package capsule.voice

import kotlin.math.sqrt

/**
 * Per-frame RMS extraction from raw PCM (CAP-QC-VOICE US-2).
 *
 * Feeds [SnrEstimator]: the estimator needs one RMS amplitude per short frame
 * (its noise-floor / voice-level split operates on frames, not on the whole
 * signal). This object turns little-endian signed 16-bit mono samples into that
 * envelope. A trailing partial frame is dropped so a short tail never biases
 * the noise floor downward.
 *
 * Pure — no I/O. The decoding half lives in [AudioEnvelopeExtractor].
 */
object RmsEnvelope {

    /**
     * @param pcm           raw little-endian signed 16-bit samples.
     * @param bytesPerSample sample width in bytes (default 2 — s16le).
     * @param frameSamples  number of samples per frame (e.g. 320 = 20 ms @ 16 kHz).
     * @return one normalised RMS amplitude in `[0, 1]` per full frame.
     */
    fun framesFromPcm(pcm: ByteArray, bytesPerSample: Int = 2, frameSamples: Int = 320): DoubleArray {
        require(bytesPerSample > 0) { "bytesPerSample must be > 0, got $bytesPerSample" }
        require(frameSamples > 0) { "frameSamples must be > 0, got $frameSamples" }

        val sampleCount = pcm.size / bytesPerSample
        val frameCount = sampleCount / frameSamples
        if (frameCount == 0) return DoubleArray(0)

        val frames = DoubleArray(frameCount)
        for (frame in 0 until frameCount) {
            var sumSquares = 0.0
            val base = frame * frameSamples * bytesPerSample
            for (sampleIndex in 0 until frameSamples) {
                val offset = base + sampleIndex * bytesPerSample
                val low = pcm[offset].toInt() and 0xFF
                val high = pcm[offset + 1].toInt() shl 8
                val sample = (high or low) / 32768.0
                sumSquares += sample * sample
            }
            frames[frame] = sqrt(sumSquares / frameSamples)
        }
        return frames
    }
}
