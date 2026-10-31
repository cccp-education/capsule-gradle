package capsule.voice

import capsule.ProcessRunner
import java.io.ByteArrayOutputStream
import java.io.File
import video.kernel.audio.RmsEnvelope
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD unit tests for [RmsEnvelope] and [AudioEnvelopeExtractor]
 * (CAP-QC-VOICE US-2). The decode is intercepted — no real ffmpeg needed.
 */
class AudioEnvelopeTest {

    @AfterTest
    fun tearDown() {
        ProcessRunner.commandInterceptor = null
    }

    private fun pcm(vararg samples: Int): ByteArray {
        val out = ByteArrayOutputStream()
        samples.forEach { s ->
            out.write(s and 0xFF)
            out.write((s shr 8) and 0xFF)
        }
        return out.toByteArray()
    }

    @Test
    fun `silence yields a zero envelope`() {
        val frames = RmsEnvelope.framesFromPcm(pcm(0, 0, 0, 0), bytesPerSample = 2, frameSamples = 2)
        assertEquals(2, frames.size)
        assertTrue(frames.all { it == 0.0 })
    }

    @Test
    fun `a full-scale frame yields the expected rms`() {
        val frames = RmsEnvelope.framesFromPcm(pcm(0, 32767), bytesPerSample = 2, frameSamples = 2)
        assertEquals(1, frames.size)
        assertEquals(0.7071, frames[0], 1e-3)
    }

    @Test
    fun `a trailing partial frame is dropped`() {
        val frames = RmsEnvelope.framesFromPcm(pcm(1000, 1000, 1000), bytesPerSample = 2, frameSamples = 2)
        assertEquals(1, frames.size)
    }

    @Test
    fun `a signal shorter than one frame yields no frame`() {
        val frames = RmsEnvelope.framesFromPcm(pcm(1000), bytesPerSample = 2, frameSamples = 2)
        assertEquals(0, frames.size)
    }

    @Test
    fun `the decode argv decodes to raw mono pcm`() {
        val audio = File("/tmp/slide-01.mp3")
        val out = File("/tmp/envelope.pcm")
        val args = AudioEnvelopeExtractor.decodeArgs(audio, out, 16000, "ffmpeg")

        assertEquals("ffmpeg", args.first())
        assertTrue("-y" in args, "ffmpeg must overwrite the temp PCM target (S-209 bug)")
        assertTrue(args.windowed(2).any { it == listOf("-ac", "1") })
        assertTrue(args.windowed(2).any { it == listOf("-ar", "16000") })
        assertTrue(args.windowed(2).any { it == listOf("-f", "s16le") })
        assertEquals(out.absolutePath, args.last())
    }

    @Test
    fun `the extractor reads the pcm ffmpeg wrote`() {
        ProcessRunner.commandInterceptor = { command, _, _, _, _ ->
            // One full 20 ms frame @ 16 kHz = 320 samples, alternating silence/peak.
            val samples = IntArray(320) { if (it % 2 == 0) 0 else 32767 }
            File(command.last()).writeBytes(pcm(*samples))
            ProcessRunner.Result(0, "")
        }
        val audio = File.createTempFile("capsule-take", ".mp3").apply { writeText("fake") }

        val frames = AudioEnvelopeExtractor.extract(audio, sampleRate = 16000, frameMillis = 20)

        assertTrue(frames != null && frames.size == 1, "expected exactly one 20 ms frame, got $frames")
    }

    @Test
    fun `a missing file degrades to null`() {
        assertNull(AudioEnvelopeExtractor.extract(File("/tmp/does-not-exist-xyz.mp3")))
    }
}
