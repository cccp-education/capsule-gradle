package capsule

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * CAP-FISH-AUDIO US-2 — response of a Fish Audio TTS transport.
 *
 * @property status the HTTP status code.
 * @property body   the raw response bytes (a WAV payload on success).
 */
data class FishAudioResponse(val status: Int, val body: ByteArray) {
    val isSuccess: Boolean get() = status in 200..299

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FishAudioResponse) return false
        return status == other.status && body.contentEquals(other.body)
    }

    override fun hashCode(): Int = 31 * status + body.contentHashCode()
}

/**
 * CAP-FISH-AUDIO US-2 — the HTTP transport seam of the Fish Audio engine.
 *
 * It exists so that **tests never call Fish Audio for real**: a test injects a
 * fake transport returning canned WAV bytes (or a non-2xx status). Production
 * uses [HttpClientFishAudioTransport] (JDK `HttpClient`, zero extra dependency).
 *
 * Implementations must not log the `headers` (they carry the `Bearer` secret).
 */
fun interface FishAudioTransport {
    fun post(endpoint: String, headers: Map<String, String>, body: ByteArray): FishAudioResponse
}

/**
 * CAP-FISH-AUDIO US-2 — production transport built on the JDK [HttpClient].
 *
 * The API key travels in the `authorization` header and is never logged.
 *
 * @param client         the shared client (defaults to a fresh one).
 * @param timeoutSeconds per-request timeout (Fish synthesis can be slow; `prepa`
 *        used 300 s).
 */
class HttpClientFishAudioTransport(
    private val client: HttpClient = HttpClient.newHttpClient(),
    private val timeoutSeconds: Long = 300,
) : FishAudioTransport {

    override fun post(endpoint: String, headers: Map<String, String>, body: ByteArray): FishAudioResponse {
        val builder = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
        headers.forEach { (name, value) -> builder.header(name, value) }
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
        return FishAudioResponse(response.statusCode(), response.body())
    }
}
