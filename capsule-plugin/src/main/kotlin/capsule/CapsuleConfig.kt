package capsule

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
    val context: ContextConfig = ContextConfig()
)

data class InputConfig(
    val outputDir: String = "capsule",
    val sliderScriptDir: String = "capsule",
    val deckSourceDir: String = "docs/asciidocRevealJs",
    val chromiumExecutablePath: String = ""
)

data class TtsConfig(
    val engine: String = "piper",
    val voice: String = "fr_FR-siwis-medium",
    val piperExecutablePath: String = "piper",
    val fallbackEnabled: Boolean = true,
    val espeakVoice: String = "fr",
    val espeakSpeed: Int = 150,
    val language: String = "fr"
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
    val subtitleBurnInPosition: String = "bottom"
)

data class DistribConfig(
    val ffmpegExecutablePath: String = "ffmpeg",
    val outputWidth: Int = 1080,
    val outputHeight: Int = 1920
)

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
 * Configuration for the versioned video destination (CAP-ARCH-7).
 *
 * The capsule pipeline writes videos to `build/<outputDir>/` during the
 * build. This config drives a *post-capture distribution* step that copies
 * the final WebM to a versioned subdirectory under [videoDestinationDir],
 * enabling the user to compare capsule versions side-by-side.
 *
 * @param videoDestinationDir absolute or relative path to the destination
 *        root (default `office/videos`, relative to the workspace root).
 * @param versioning           the versioning strategy (default [TIMESTAMP]).
 * @param versionPrefix        prefix prepended to the version label
 *        (default `v`).
 */
data class OutputConfig(
    val videoDestinationDir: String = "office/videos",
    val versioning: VersioningStrategy = VersioningStrategy.TIMESTAMP,
    val versionPrefix: String = "v"
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