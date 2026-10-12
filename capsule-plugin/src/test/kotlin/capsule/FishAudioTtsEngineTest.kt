package capsule

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * CAP-FISH-AUDIO US-2 — [FishAudioTtsEngine].
 *
 * **Fish Audio is never called**: a [FishAudioTransport] stub returns canned
 * bytes (or a failure status). These are pure-domain unit tests — no network,
 * no API key, no FFmpeg (WAV is copied under the `.mp3` name when FFmpeg is
 * absent, so the output existence is still assertable).
 */
class FishAudioTtsEngineTest {

    @field:TempDir
    lateinit var dir: File

    private class RecordingTransport(
        private val responses: List<FishAudioResponse>,
    ) : FishAudioTransport {
        val endpoints = mutableListOf<String>()
        val headers = mutableListOf<Map<String, String>>()
        val bodies = mutableListOf<ByteArray>()

        override fun post(endpoint: String, headers: Map<String, String>, body: ByteArray): FishAudioResponse {
            this.endpoints += endpoint
            this.headers += headers
            this.bodies += body
            return responses[minOf(this.bodies.size - 1, responses.size - 1)]
        }
    }

    private fun params() = FishAudioParams(voice = "voice-123")

    private fun wav(bytes: Int = 64) = ByteArray(bytes) { 7 }

    @Test
    fun `isAvailable is false without any key`() {
        val engine = FishAudioTtsEngine(params(), emptyList(), transport = RecordingTransport(listOf()))
        assertFalse(engine.isAvailable())
        assertEquals("fish", engine.name())
    }

    @Test
    fun `isAvailable is true with a non-blank key`() {
        val engine = FishAudioTtsEngine(params(), listOf(FishAudioKey("k")), transport = RecordingTransport(listOf()))
        assertTrue(engine.isAvailable())
    }

    @Test
    fun `synthesize posts the body and headers then writes the take`() {
        val transport = RecordingTransport(listOf(FishAudioResponse(200, wav())))
        val engine = FishAudioTtsEngine(params(), listOf(FishAudioKey("sk-fish-secret")), transport = transport)
        val output = File(dir, "slide-01.mp3")

        engine.synthesize("Bonjour le monde", output)

        assertEquals("https://api.fish.audio/v1/tts", transport.endpoints.single())
        val headers = transport.headers.single()
        assertEquals("Bearer sk-fish-secret", headers["authorization"])
        assertEquals("speech-1.6", headers["model"])
        assertEquals("application/json", headers["content-type"])

        val body = ObjectMapper().readTree(transport.bodies.single())
        assertEquals("Bonjour le monde", body["text"].asText())
        assertEquals("voice-123", body["reference_id"].asText())
        assertEquals("wav", body["format"].asText())
        assertEquals(0.85, body["temperature"].asDouble())
        assertEquals(0.88, body["top_p"].asDouble())

        assertTrue(output.exists(), "expected the take at ${output.path}")
        assertTrue(output.length() > 0, "expected a non-empty take")
    }

    @Test
    fun `synthesize retries on a non-2xx status then succeeds`() {
        val transport = RecordingTransport(
            listOf(FishAudioResponse(503, ByteArray(0)), FishAudioResponse(200, wav())),
        )
        val engine = FishAudioTtsEngine(params(), listOf(FishAudioKey("k")), transport = transport)
        val output = File(dir, "slide-02.mp3")

        engine.synthesize("Bonjour", output)

        assertEquals(2, transport.bodies.size, "expected one retry")
        assertTrue(output.exists())
    }

    @Test
    fun `synthesize fails after the bounded attempts`() {
        val transport = RecordingTransport(listOf(FishAudioResponse(500, ByteArray(0))))
        val engine = FishAudioTtsEngine(params(), listOf(FishAudioKey("k")), transport = transport, maxAttempts = 2)

        val error = assertFailsWith<TtsException> {
            engine.synthesize("Bonjour", File(dir, "slide-03.mp3"))
        }
        assertContains(error.message!!, "failed after 2 attempt")
        assertEquals(2, transport.bodies.size)
    }

    @Test
    fun `synthesize without a key is a clear failure`() {
        val engine = FishAudioTtsEngine(params(), emptyList(), transport = RecordingTransport(listOf()))
        val error = assertFailsWith<TtsException> { engine.synthesize("Bonjour", File(dir, "slide-04.mp3")) }
        assertContains(error.message!!, "no API key")
    }

    @Test
    fun `synthesize with a blank voice is a clear failure`() {
        val engine = FishAudioTtsEngine(
            FishAudioParams(voice = ""),
            listOf(FishAudioKey("k")),
            transport = RecordingTransport(listOf(FishAudioResponse(200, wav()))),
        )
        val error = assertFailsWith<TtsException> { engine.synthesize("Bonjour", File(dir, "slide-05.mp3")) }
        assertContains(error.message!!, "fishVoice")
    }

    @Test
    fun `the key never leaks through the engine rendering`() {
        val engine = FishAudioTtsEngine(params(), listOf(FishAudioKey("sk-fish-secret")), transport = RecordingTransport(listOf()))
        assertFalse(engine.toString().contains("sk-fish-secret"))
    }
}
