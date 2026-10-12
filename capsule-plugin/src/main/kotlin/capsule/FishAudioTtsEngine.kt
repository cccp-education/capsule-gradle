package capsule

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

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
 * Retries are bounded (transient transport hiccups); the write is atomic
 * (`.part` → [FileReplace.moveOver]) so a failed call never leaves a truncated
 * take. Idempotence (skip an already-synthesised take) is the caller's job
 * (the CAP-QC TTS cache).
 *
 * @param params      non-secret parameters (voice/model/format/…).
 * @param keys        the `fish-audio` key pool (first non-blank key is used).
 * @param ffmpegPath  the FFmpeg binary for the WAV → MP3 conversion.
 * @param transport   the HTTP seam (fake in tests).
 * @param maxAttempts bounded retry count.
 */
class FishAudioTtsEngine(
    private val params: FishAudioParams,
    private val keys: List<FishAudioKey>,
    private val ffmpegPath: String = "ffmpeg",
    private val transport: FishAudioTransport = HttpClientFishAudioTransport(),
    private val maxAttempts: Int = 3,
) : TtsEngine {

    private val mapper = ObjectMapper().registerKotlinModule()

    override fun isAvailable(): Boolean = keys.any { it.key.isNotBlank() }

    override fun name(): String = "fish"

    override fun synthesize(text: String, outputFile: File) {
        val key = keys.firstOrNull { it.key.isNotBlank() }?.key
            ?: throw TtsException("Fish Audio: no API key (the fish-audio pool is empty)")

        if (params.voice.isBlank()) {
            throw TtsException("Fish Audio: tts.fishVoice (the reference_id) must not be blank")
        }

        val body = mapper.writeValueAsBytes(
            FishAudioRequestBody(
                text = text,
                referenceId = params.voice,
                format = params.format,
                temperature = params.temperature,
                topP = params.topP,
            )
        )
        val headers = mapOf(
            "authorization" to "Bearer $key",
            "content-type" to "application/json",
            "model" to params.model,
        )

        val wavFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + ".wav")
        wavFile.parentFile.mkdirs()

        var lastError = "unknown error"
        for (attempt in 1..maxAttempts) {
            try {
                val response = transport.post(params.endpoint, headers, body)
                if (response.isSuccess && response.body.isNotEmpty()) {
                    val tmp = File(wavFile.parentFile, wavFile.name + ".part")
                    tmp.writeBytes(response.body)
                    if (!FileReplace.moveOver(tmp, wavFile)) {
                        throw TtsException("Fish Audio: could not materialise the WAV take")
                    }
                    AudioConversionUtil.wavToMp3(wavFile, outputFile, ffmpegPath)
                    wavFile.delete()
                    return
                }
                lastError = "HTTP ${response.status}"
            } catch (e: Exception) {
                lastError = e.message ?: e.toString()
            }
        }
        throw TtsException("Fish Audio synthesis failed after $maxAttempts attempt(s): $lastError")
    }
}
