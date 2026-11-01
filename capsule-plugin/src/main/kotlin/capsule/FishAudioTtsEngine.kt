package capsule

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.resilience.Schedule
import arrow.resilience.retryEither
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * CAP-FISH-AUDIO — the non-secret parameters of the Fish Audio engine.
 *
 * They come from the `tts` config section (`tts.fishVoice`, `tts.fishModel`,
 * `tts.fishFormat`, `tts.fishTemperature`, `tts.fishTopP`, `tts.fishEndpoint`)
 * — the API key lives apart, in the git-ignored `fish.yaml`.
 *
 * Defaults mirror the `prepa` reference (`gen_audio.py`): `speech-1.6`, WAV (the
 * default MP3 adds a compression generation), a warm register (0.85 / 0.88).
 *
 * @property voice       the cloned voice id sent as `reference_id` (required).
 * @property model       the Fish model header (default `speech-1.6`).
 * @property format      the requested container (default `wav`).
 * @property temperature sampling temperature (default `0.85`).
 * @property topP        nucleus sampling (default `0.88`).
 * @property endpoint    the TTS endpoint (default `https://api.fish.audio/v1/tts`).
 */
data class FishAudioParams(
    val voice: String,
    val model: String = "speech-1.6",
    val format: String = "wav",
    val temperature: Double = 0.85,
    val topP: Double = 0.88,
    val endpoint: String = "https://api.fish.audio/v1/tts",
)

/** Request body of a Fish Audio `/v1/tts` call (JSON). */
private data class FishAudioRequestBody(
    val text: String,
    @JsonProperty("reference_id") val referenceId: String,
    val format: String,
    val temperature: Double,
    @JsonProperty("top_p") val topP: Double,
)

/**
 * XF-FP2-4 — outcome of **one** synthesis attempt, used as the [Schedule] input.
 *
 * Stdlib only: it is the N2 translation of a transport/HTTP hiccup into a value,
 * so retries are data-driven instead of an imperative `try/catch` loop.
 */
internal sealed interface FishFailure {
    /** A stable, non-secret failure reason (safe to put in an exception message). */
    val reason: String

    /** The service answered, but not with a usable WAV take. */
    data class HttpStatus(
        val status: Int,
    ) : FishFailure {
        override val reason: String get() = "HTTP $status"
    }

    /** The transport itself failed (connection reset, timeout, DNS, …). */
    data class Transport(
        override val reason: String,
    ) : FishFailure
}

/**
 * CAP-FISH-AUDIO US-2 — the Fish Audio cloud TTS engine (cloned voice).
 *
 * A 4th [TtsEngine], additive: Piper/Espeak/NoOp are untouched. It POSTs a JSON
 * body to [FishAudioParams.endpoint] with the `Bearer` key, requests a WAV
 * (`prepa` lesson: the default MP3 adds a compression generation) and converts
 * it to the pipeline MP3 via [AudioConversionUtil.wavToMp3].
 *
 * The API key is a **secret**: it is never logged, and it is redacted through
 * [FishAudioKey]. Tests **never** call Fish Audio — they inject a
 * [FishAudioTransport] stub returning canned bytes.
 *
 * Retries are bounded and **exponential** (XF-FP2-4): the imperative busy loop
 * is replaced by an Arrow [Schedule] (`exponential` jittered, capped by
 * `recurs`), so a transient hiccup waits ~250 ms then ~500 ms instead of
 * hammering the endpoint immediately. `maxAttempts` keeps its meaning: the
 * **total** number of attempts (the first call included). The write is atomic
 * (`.part` → [FileReplace.moveOver]) so a failed call never leaves a truncated
 * take; file materialisation and MP3 conversion only run on the success path.
 * Idempotence (skip an already-synthesised take) is the caller's job (the
 * CAP-QC TTS cache).
 *
 * @param params      non-secret parameters (voice/model/format/…).
 * @param keys        the `fish-audio` key pool (first non-blank key is used).
 * @param ffmpegPath  the FFmpeg binary for the WAV → MP3 conversion.
 * @param transport   the HTTP seam (fake in tests).
 * @param maxAttempts total number of attempts, the first call included.
 * @param retryDelay  test seam mapping a schedule delay to the actual wait;
 *                    the default keeps the backoff, tests pass `{ Duration.ZERO }`.
 */
class FishAudioTtsEngine(
    private val params: FishAudioParams,
    private val keys: List<FishAudioKey>,
    private val ffmpegPath: String = "ffmpeg",
    private val transport: FishAudioTransport = HttpClientFishAudioTransport(),
    private val maxAttempts: Int = 3,
    internal val retryDelay: (Duration) -> Duration = { it },
) : TtsEngine {
    private val mapper = ObjectMapper().registerKotlinModule()

    override fun isAvailable(): Boolean = keys.any { it.key.isNotBlank() }

    override fun name(): String = "fish"

    override fun synthesize(
        text: String,
        outputFile: File,
    ) {
        val key =
            keys.firstOrNull { it.key.isNotBlank() }?.key
                ?: throw TtsException("Fish Audio: no API key (the fish-audio pool is empty)")

        if (params.voice.isBlank()) {
            throw TtsException("Fish Audio: tts.fishVoice (the reference_id) must not be blank")
        }

        val body =
            mapper.writeValueAsBytes(
                FishAudioRequestBody(
                    text = text,
                    referenceId = params.voice,
                    format = params.format,
                    temperature = params.temperature,
                    topP = params.topP,
                ),
            )
        val headers =
            mapOf(
                "authorization" to "Bearer $key",
                "content-type" to "application/json",
                "model" to params.model,
            )

        val wavFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + ".wav")
        wavFile.parentFile.mkdirs()

        val bytes =
            runBlocking {
                retryPolicy().retryEither {
                    attemptOnce(params.endpoint, headers, body)
                }
            }.getOrElse { failure ->
                throw TtsException("Fish Audio synthesis failed after $maxAttempts attempt(s): ${failure.reason}")
            }

        val tmp = File(wavFile.parentFile, wavFile.name + ".part")
        tmp.writeBytes(bytes)
        if (!FileReplace.moveOver(tmp, wavFile)) {
            throw TtsException("Fish Audio: could not materialise the WAV take")
        }
        AudioConversionUtil.wavToMp3(wavFile, outputFile, ffmpegPath)
        wavFile.delete()
    }

    /**
     * One Fish Audio attempt as a value.
     *
     * Transport/HTTP hiccups become a [FishFailure] (retryable); a cancellation
     * is rethrown untouched, mirroring Arrow's `Either.catch` semantics.
     */
    private suspend fun attemptOnce(
        endpoint: String,
        headers: Map<String, String>,
        body: ByteArray,
    ): Either<FishFailure, ByteArray> {
        val response =
            try {
                transport.post(endpoint, headers, body)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return FishFailure.Transport(e.message ?: e.toString()).left()
            }

        return if (response.isSuccess && response.body.isNotEmpty()) {
            response.body.right()
        } else {
            FishFailure.HttpStatus(response.status).left()
        }
    }

    /**
     * Exponential backoff capped by [maxAttempts].
     *
     * Arrow runs the action **once** before consulting the schedule
     * (`InvocationKind.AT_LEAST_ONCE`), so `recurs(n)` adds `n` retries: `n` is
     * `maxAttempts - 1` to keep `maxAttempts` a **total** count.
     */
    private fun retryPolicy(): Schedule<FishFailure, *> {
        val retries = (maxAttempts - 1).coerceAtLeast(0).toLong()
        return (
            Schedule.exponential<FishFailure>(RETRY_BASE_DELAY).jittered() and
                Schedule.recurs<FishFailure>(retries)
        ).delayed { _, delay -> retryDelay(delay) }
    }

    private companion object {
        /** First backoff step; each subsequent one doubles (~250 ms, ~500 ms, …). */
        val RETRY_BASE_DELAY: Duration = 250.milliseconds
    }
}
