package capsule

import capsule.audio.AudioPostConfig
import capsule.chapters.ChaptersConfig
import capsule.podcast.PodcastConfig
import capsule.preview.PreviewConfig
import capsule.publish.PublishConfig
import capsule.transcript.TranscriptConfig
import capsule.viral.ViralConfig
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Immutable configuration model for capsule-gradle.
 *
 * Loaded from YAML, merged from 4 sources (ENV < gradle.properties < YAML < CLI).
 * Maps 1:1 with the `capsule-context.yml` file structure.
 *
 * Resolution order: ENV vars < gradle.properties < YAML file < CLI -P params
 */
data class CapsuleConfig(
    val input: InputConfig = InputConfig(),
    val tts: TtsConfig = TtsConfig(),
    val capture: CaptureConfig = CaptureConfig(),
    val distrib: DistribConfig = DistribConfig(),
    val manim: ManimConfig = ManimConfig(),
    val output: OutputConfig = OutputConfig(),
    val strictMode: StrictModeConfig = StrictModeConfig(),
    val context: ContextConfig = ContextConfig(),
    val validation: ValidationConfig = ValidationConfig(),
    val qc: QcConfig = QcConfig(),
    val audioPost: AudioPostConfig = AudioPostConfig(),
    val transcript: TranscriptConfig = TranscriptConfig(),
    val remotion: RemotionConfig = RemotionConfig(),
    val podcast: PodcastConfig = PodcastConfig(),
    val preview: PreviewConfig = PreviewConfig(),
    val chapters: ChaptersConfig = ChaptersConfig(),
    val viral: ViralConfig = ViralConfig(),
    val voice: VoiceConfig = VoiceConfig(),
    /**
     * HF-YOUTUBE-PUBLISH US-3b — the `publish` section: automated upload to a
     * video platform (YouTube) through the shared N1 `VideoPublisher` port.
     * Disabled by default.
     */
    val publish: PublishConfig = PublishConfig(),
    /**
     * CAP-FISH-AUDIO — the `fish-audio` member: a pool of Fish Audio API keys,
     * loaded from the git-ignored `fish.yaml` (never from the committed
     * `capsule-context.yml`). Empty when the file is absent — the `fish` engine is
     * then unavailable (degraded, never a failure). The hyphenated YAML key
     * requires the explicit [JsonProperty].
     */
    @JsonProperty("fish-audio")
    val fishAudio: List<FishAudioKey> = emptyList()
)

/**
 * Configuration section for voice-quality verification (CAP-QC-VOICE US-2).
 *
 * When [verifyEnabled] is true, `verifyCapsuleVoice` transcribes every
 * `slide-NN.mp3` through the ASR port and checks it against the text that was
 * meant to be spoken: fidelity (word error rate), hallucination (surplus words)
 * and background headroom (SNR). The ASR binary is **never** a prerequisite of
 * `check` — when it is absent the task degrades (unless `strictMode`).
 *
 * Default is disabled to preserve backward compatibility.
 *
 * @param verifyEnabled      `true` to run the QC voice check (default `false`).
 * @param maxWordErrorRate   word error rate ceiling (default 0.15).
 * @param minSnrDb           background headroom floor in dB (default 66.0).
 * @param surplusThreshold   extra transcribed words signalling a hallucination (default 3).
 * @param asrExecutablePath  the ASR binary (`whisper`, `faster-whisper`…).
 * @param asrModel           the ASR model name, blank for the CLI default.
 * @param language           the ASR language hint (ISO-639-1, default `fr`).
 * @param selectBestTake     `true` to run the take selection (`selectBestVoiceTake`,
 *        US-3) when a slide was synthesised more than once (default `false`).
 */
data class VoiceConfig(
    val verifyEnabled: Boolean = false,
    val maxWordErrorRate: Double = 0.15,
    val minSnrDb: Double = 66.0,
    val surplusThreshold: Int = 3,
    val asrExecutablePath: String = "whisper",
    val asrModel: String = "",
    val language: String = "fr",
    val selectBestTake: Boolean = false,
)

/**
 * Configuration section for content pre-flight (PRT-05).
 *
 * When [preflightEnabled] is true, `preflightCapsuleContent` runs a structural
 * pass over the capsule scripts BEFORE any metered TTS synthesis: over-long
 * notes and duplicated notes warn, forbidden forms fail. Default is disabled to
 * preserve backward compatibility.
 *
 * @param preflightEnabled `true` to run the pre-flight (default `false`).
 * @param maxNoteChars     maximum characters per speaker note before a warning
 *        (default `500`).
 */
data class QcConfig(
    val preflightEnabled: Boolean = false,
    val maxNoteChars: Int = 500,
    val boardsEnabled: Boolean = false,
)

data class InputConfig(
    val outputDir: String = "capsule",
    val sliderScriptDir: String = "capsule",
    val deckSourceDir: String = "docs/asciidocRevealJs",
    val sliderBuildDir: String = "",
    val chromiumExecutablePath: String = ""
)

data class TtsConfig(
    val engine: String = "piper",
    val voice: String = "fr_FR-siwis-medium",
    val piperExecutablePath: String = "piper",
    val fallbackEnabled: Boolean = true,
    val espeakVoice: String = "fr",
    val espeakSpeed: Int = 150,
    val language: String = "fr",
    /**
     * PRT-01 — `true` to speak the "dit / lu" pronunciation form (acronyms spelled,
     * symbols expanded) and index the TTS cache on the spoken text (`<file>.key`).
     * Defaults to false (backward compat — the display text is spoken verbatim).
     */
    val pronunciationEnabled: Boolean = false,
    /**
     * CAP-FISH-AUDIO — the cloned voice id (`reference_id`) for the `fish` engine.
     * Empty by default: the `fish` engine requires it (fail-fast otherwise). The
     * API key lives apart, in the git-ignored `fish.yaml`.
     */
    val fishVoice: String = "",
    /** CAP-FISH-AUDIO — the Fish model header (default `speech-1.6`). */
    val fishModel: String = "speech-1.6",
    /** CAP-FISH-AUDIO — the requested container (default `wav` — MP3 adds a compression generation). */
    val fishFormat: String = "wav",
    /** CAP-FISH-AUDIO — sampling temperature (default `0.85`, warm register). */
    val fishTemperature: Double = 0.85,
    /** CAP-FISH-AUDIO — nucleus sampling (default `0.88`). */
    val fishTopP: Double = 0.88,
    /** CAP-FISH-AUDIO — the TTS endpoint (default `https://api.fish.audio/v1/tts`). */
    val fishEndpoint: String = "https://api.fish.audio/v1/tts"
)

data class CaptureConfig(
    val viewportWidth: Int = 1408,
    val viewportHeight: Int = 792,
    val playwrightTimeout: Double = 120_000.0,
    val slideDurationSeconds: Double = 5.0,
    val parallelCaptureEnabled: Boolean = false,
    val parallelCaptureThreads: Int = 4,
    val captureTimeoutMinutes: Int = 5,
    val subtitleEnabled: Boolean = false,
    val subtitleFormat: String = "srt",
    val subtitleBurnIn: Boolean = false,
    val subtitleBurnInFontSize: Int = 24,
    val subtitleBurnInFontColor: String = "&H00FFFFFF",
    val subtitleBurnInOutlineColor: String = "&H00000000",
    val subtitleBurnInPosition: String = "bottom",
    val subtitleCartonsEnabled: Boolean = false,
    val subtitleMaxChars: Int = 42,
    val reuseCapturedVideoEnabled: Boolean = false,
    val strategy: CaptureStrategy = CaptureStrategy.PLAYWRIGHT
)

data class DistribConfig(
    val ffmpegExecutablePath: String = "ffmpeg",
    val outputWidth: Int = 1080,
    val outputHeight: Int = 1920
)

/**
 * Capture strategy selector (CAP-CR3-3 US-1).
 *
 * - [PLAYWRIGHT] — Playwright real-time video recording (default, backward compat).
 *   Records the deck navigation as a single WebM via Playwright's
 *   `recordVideoDir` API.
 * - [SCREENSHOT] — Screenshot-based capture: takes a PNG screenshot of each
 *   slide then uses FFmpeg to produce a WebM of the exact audio duration per
 *   slide, followed by a concat. Orders of magnitude faster and more reliable
 *   than Playwright real-time recording.
 * - [REMOTION] — Frame-by-frame rendering through Remotion. The deck sections
 *   are handed to a React composition that animates them (entrances, drift,
 *   cross-fades) instead of holding a still image for the whole slide. Costs
 *   more CPU than [SCREENSHOT] but is the only strategy that produces motion,
 *   and it parallelises across cores natively.
 */
enum class CaptureStrategy {
    PLAYWRIGHT,
    SCREENSHOT,
    REMOTION;

    companion object {
        /**
         * Case-insensitive parse. Falls back to [PLAYWRIGHT] for
         * null/blank/unknown values (backward compat — existing configs
         * without `capture.strategy` keep the Playwright behavior).
         */
        fun fromString(value: String?): CaptureStrategy =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PLAYWRIGHT
    }
}

/**
 * Versioning strategy for video destination copies (CAP-ARCH-7).
 *
 * - [TIMESTAMP] — each copy is versioned with an ISO-8601 timestamp
 *   (`v2026-08-10T0423`). Idempotent, no state to manage.
 * - [INCREMENTAL] — copies are versioned with an incrementing integer
 *   (`v1`, `v2`, ...). Requires reading the destination directory to
 *   determine the next version.
 */
enum class VersioningStrategy {
    TIMESTAMP,
    INCREMENTAL;

    companion object {
        fun fromString(value: String?): VersioningStrategy =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TIMESTAMP
    }
}

/**
 * Output format selector for capsule videos (CAP-MP4 US-1).
 *
 * - [WEBM] — WebM only (default, backward compat — existing configs without
 *   `output.format` keep the WebM-only behavior).
 * - [MP4] — transcode to MP4/H.264 via FFmpeg post-capture, then remove the
 *   WebM intermediate. LMS (Moodle, Canvas), YouTube, and OF platforms
 *   consume MP4/H.264/AAC natively.
 * - [BOTH] — keep the WebM intermediate and produce the MP4 alongside.
 */
enum class OutputFormat {
    WEBM,
    MP4,
    BOTH;

    companion object {
        /**
         * Case-insensitive parse. Falls back to [WEBM] for
         * null/blank/unknown values (backward compat — existing configs
         * without `output.format` keep the WebM-only behavior).
         */
        fun fromString(value: String?): OutputFormat =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WEBM
    }
}

/**
 * Configuration for the versioned video destination (CAP-ARCH-7) and output
 * format (CAP-MP4 US-1).
 *
 * The capsule pipeline writes videos to `build/<outputDir>/` during the
 * build. This config drives a *post-capture distribution* step that copies
 * the final video to a versioned subdirectory under [videoDestinationDir],
 * enabling the user to compare capsule versions side-by-side. The [format]
 * field selects the output container (WebM, MP4, or both).
 *
 * @param videoDestinationDir absolute or relative path to the destination
 *        root (default `office/videos`, relative to the workspace root).
 * @param versioning           the versioning strategy (default [TIMESTAMP]).
 * @param versionPrefix        prefix prepended to the version label
 *        (default `v`).
 * @param format               the output container format
 *        (default [OutputFormat.WEBM] — backward compat).
 */
data class OutputConfig(
    val videoDestinationDir: String = "office/videos",
    val versioning: VersioningStrategy = VersioningStrategy.TIMESTAMP,
    val versionPrefix: String = "v",
    val format: OutputFormat = OutputFormat.WEBM,
    /** PRT-09 residual — tag the MP4 as BT.709 (broadcast colorimetry). Opt-in, default false. */
    val bt709: Boolean = false
)

data class ManimConfig(
    val executablePath: String = "manim",
    val quality: String = "l",
    val scriptsDir: String = "src/manim",
    val outputDir: String = "build/capsule/manim",
    val parallelRender: Boolean = false,
    val parallelRenderThreads: Int = 4
) {
    companion object {
        /**
         * Valid Manim quality flags (manim -q{l|m|h|p|k}).
         *
         * | Flag | Resolution | FPS |
         * |------|------------|-----|
         * | l    | 480p       | 60  |
         * | m    | 720p       | 60  |
         * | h    | 1080p      | 60  |
         * | p    | 1440p      | 60  |
         * | k    | 2160p (4K)  | 60  |
         */
        val VALID_QUALITIES = setOf("l", "m", "h", "p", "k")
    }

    /**
     * Validates ManimConfig fields. Returns a list of error messages.
     * Empty list means the config is valid.
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (executablePath.isBlank() && executablePath != "noop") {
            errors.add("executablePath must not be blank (use 'noop' for testing)")
        }
        when {
            quality == "noop" -> {} // valid, no error
            quality.isBlank() -> errors.add("quality must not be blank")
            quality !in VALID_QUALITIES -> errors.add("quality '$quality' is not a valid Manim quality flag. Valid: ${VALID_QUALITIES.joinToString(", ")}")
        }
        if (scriptsDir.isBlank()) {
            errors.add("scriptsDir must not be blank")
        }
        return errors
    }
}

/**
 * Configuration section for video duration validation (CAP-CR3-1 US-2).
 *
 * When [durationEnabled] is true, the `validateCapsuleVideoDuration`
 * task probes the final video duration and compares it against the sum
 * of the per-slide TTS audio durations. A delta exceeding
 * [toleranceSecs] fails the build — preventing the "false positive"
 * where `check` is green but the produced video is truncated or
 * misaligned.
 *
 * Default is disabled to preserve backward compatibility — existing
 * configs without a `validation` section keep the no-validation
 * behavior.
 *
 * @param durationEnabled  `true` to enable duration validation
 *        (default `false` — backward compat).
 * @param toleranceSecs    the tolerance threshold in seconds
 *        (default `2.0` — realistic for ffmpeg mux drift and
 *        Playwright capture latency).
 * @param integrityEnabled `true` to enable decode-validity validation: the
 *        final media must fully DECODE (PRT-04 — a broken container keeps a
 *        plausible duration yet cannot be reused). Default `false`.
 */
data class ValidationConfig(
    val durationEnabled: Boolean = false,
    val toleranceSecs: Double = 2.0,
    val integrityEnabled: Boolean = false
)

/**
 * Configuration section for the Remotion capture strategy (CAP-ANIM).
 *
 * Remotion renders the deck frame by frame from a React composition, which is
 * what makes animation possible: entrance transitions, slow drift and
 * cross-fades between slides, instead of one frozen screenshot per slide.
 *
 * The plugin ships the composition itself — consumers do not author React. It
 * is materialised into [projectDir] on first use, and the deck sections are
 * passed to it as props, so a deck keeps its own CSS and identity.
 *
 * Defaults keep the strategy inert: it only runs when
 * `capture.strategy=remotion` is selected.
 *
 * @param projectDir      where the bundled composition is materialised,
 *        relative to the build directory (default `capsule/remotion`).
 * @param nodeExecutablePath the `node` binary driving the render (default `node`).
 * @param concurrency     how many frames are rendered in parallel. Remotion runs
 *        one browser tab per unit, so this is the real multi-core knob
 *        (default 4).
 * @param fps             frame rate of the produced video (default 30).
 */
data class RemotionConfig(
    val projectDir: String = "capsule/remotion",
    val nodeExecutablePath: String = "node",
    val concurrency: Int = 4,
    val fps: Int = 30,
) {
    /** Validates the section. Returns the error messages, empty when valid. */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (projectDir.isBlank()) errors.add("projectDir must not be blank")
        if (nodeExecutablePath.isBlank()) errors.add("nodeExecutablePath must not be blank")
        if (concurrency < 1) errors.add("concurrency must be >= 1, got $concurrency")
        if (fps !in 1..120) errors.add("fps must be between 1 and 120, got $fps")
        return errors
    }
}