package capsule.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [AudioSilenceDetector] (CAP-AUDIO-SILENCE US-1).
 *
 * Pure — no I/O, no ASR, no ffmpeg. The detector works on the RMS envelope
 * produced by [RmsEnvelope] (or any equivalent frame reader).
 */
class AudioSilenceDetectorTest {

    @Test
    fun `digital silence is silent`() {
        val analysis = AudioSilenceDetector.analyze(DoubleArray(50) { 0.0 })

        assertTrue(analysis.isSilent)
        assertEquals(50, analysis.totalFrames)
        assertEquals(50, analysis.silentFrames)
        assertEquals(1.0, analysis.silentFraction)
        assertEquals(0.0, analysis.peakRms)
    }

    @Test
    fun `an empty envelope is silent`() {
        val analysis = AudioSilenceDetector.analyze(DoubleArray(0))

        assertTrue(analysis.isSilent)
        assertEquals(0, analysis.totalFrames)
        assertEquals(1.0, analysis.silentFraction)
    }

    @Test
    fun `one audible frame is not silent`() {
        val frames = DoubleArray(50) { 0.0 }
        frames[25] = 0.2

        val analysis = AudioSilenceDetector.analyze(frames)

        assertFalse(analysis.isSilent)
        assertEquals(0.2, analysis.peakRms)
        assertEquals(49, analysis.silentFrames)
    }

    @Test
    fun `a frame at the threshold counts as silent`() {
        val analysis = AudioSilenceDetector.analyze(
            doubleArrayOf(AudioSilenceDetector.DEFAULT_RMS_THRESHOLD),
        )

        assertTrue(analysis.isSilent)
    }

    @Test
    fun `a frame above the threshold is audible`() {
        val analysis = AudioSilenceDetector.analyze(
            doubleArrayOf(AudioSilenceDetector.DEFAULT_RMS_THRESHOLD * 2.0),
        )

        assertFalse(analysis.isSilent)
    }

    @Test
    fun `a custom threshold is honoured`() {
        val frames = doubleArrayOf(0.0, 0.004, 0.0)

        assertFalse(AudioSilenceDetector.analyze(frames, threshold = 0.001).isSilent)
        assertTrue(AudioSilenceDetector.analyze(frames, threshold = 0.005).isSilent)
    }

    @Test
    fun `the silent fraction is reported for a mostly silent take`() {
        val frames = DoubleArray(100) { 0.0 }
        frames[0] = 0.2
        frames[1] = 0.2

        val analysis = AudioSilenceDetector.analyze(frames)

        assertEquals(0.98, analysis.silentFraction)
        assertFalse(analysis.isSilent, "a take with 2% audible frames is not digital silence")
    }
}
