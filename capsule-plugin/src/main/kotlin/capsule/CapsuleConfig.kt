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
    val manim: ManimConfig = ManimConfig()
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
    val espeakSpeed: Int = 150
)

data class CaptureConfig(
    val viewportWidth: Int = 1408,
    val viewportHeight: Int = 792,
    val playwrightTimeout: Double = 120_000.0,
    val slideDurationSeconds: Double = 5.0,
    val parallelCaptureEnabled: Boolean = false,
    val parallelCaptureThreads: Int = 4
)

data class DistribConfig(
    val ffmpegExecutablePath: String = "ffmpeg",
    val outputWidth: Int = 1080,
    val outputHeight: Int = 1920
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
        if (quality.isNotBlank() && quality != "noop" && quality !in VALID_QUALITIES) {
            errors.add("quality '$quality' is not a valid Manim quality flag. Valid: ${VALID_QUALITIES.joinToString(", ")}")
        }
        if (scriptsDir.isBlank()) {
            errors.add("scriptsDir must not be blank")
        }
        // "noop" is a special quality for NoOpManimEngine — skip quality validation
        if (quality == "noop") {
            // noop is valid, no error
        } else if (quality.isBlank()) {
            errors.add("quality must not be blank")
        }
        return errors
    }
}