package capsule

import java.io.File

/**
 * Merges configuration from four sources with priority:
 * ENV vars < gradle.properties < YAML file < CLI -P params
 *
 * Pattern aligné sur plantuml-gradle ConfigMerger.
 * Each higher-priority source overrides the same key from lower-priority sources.
 */
object CapsuleConfigMerger {

    /**
     * Merges configuration from all four sources.
     *
     * @param projectDir The project directory (where gradle.properties lives)
     * @param yamlConfig The configuration loaded from the YAML file
     * @param cliParams  CLI -P params as a flat map (e.g. "tts.engine" -> "piper")
     * @param yamlLoaded Whether the YAML file was actually found and loaded.
     *                   When false, YAML values are ignored and props/ENV take precedence.
     * @return The merged CapsuleConfig with all sources resolved
     */
    fun merge(projectDir: File, yamlConfig: CapsuleConfig, cliParams: Map<String, Any?>, yamlLoaded: Boolean = true): CapsuleConfig {
        val propertiesConfig = loadFromGradleProperties(projectDir)
        val envConfig = loadFromEnvironment()

        if (!yamlLoaded) {
            // No YAML file was found — fallback to props > ENV, with CLI on top
            return CapsuleConfig(
                input = mergeInputConfigNoYaml(envConfig.input, propertiesConfig.input, cliParams),
                tts = mergeTtsConfigNoYaml(envConfig.tts, propertiesConfig.tts, cliParams),
                capture = mergeCaptureConfigNoYaml(envConfig.capture, propertiesConfig.capture, cliParams),
                distrib = mergeDistribConfigNoYaml(envConfig.distrib, propertiesConfig.distrib, cliParams),
                manim = mergeManimConfigNoYaml(envConfig.manim, propertiesConfig.manim, cliParams)
            )
        }

        return CapsuleConfig(
            input = mergeInputConfig(envConfig.input, propertiesConfig.input, yamlConfig.input, cliParams),
            tts = mergeTtsConfig(envConfig.tts, propertiesConfig.tts, yamlConfig.tts, cliParams),
            capture = mergeCaptureConfig(envConfig.capture, propertiesConfig.capture, yamlConfig.capture, cliParams),
            distrib = mergeDistribConfig(envConfig.distrib, propertiesConfig.distrib, yamlConfig.distrib, cliParams),
            manim = mergeManimConfig(envConfig.manim, propertiesConfig.manim, yamlConfig.manim, cliParams)
        )
    }

    /**
     * Loads configuration from gradle.properties in the project directory.
     * Only reads properties prefixed with "capsule.".
     */
    internal fun loadFromGradleProperties(projectDir: File): CapsuleConfig {
        val props = mutableMapOf<String, String>()

        val propertiesFile = File(projectDir, "gradle.properties")
        if (propertiesFile.exists()) {
            propertiesFile.reader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("capsule.") && !trimmed.startsWith("#")) {
                        val parts = trimmed.split("=", limit = 2)
                        if (parts.size == 2) {
                            props[parts[0].trim()] = parts[1].trim()
                        }
                    }
                }
            }
        }

        return buildConfigFromProperties(props)
    }

    /**
     * Loads configuration from environment variables prefixed with CAPSULE_.
     *
     * Convention: CAPSULE_TTS_ENGINE → tts.engine, CAPSULE_CAPTURE_VIEWPORT_WIDTH → capture.viewportWidth
     */
    internal fun loadFromEnvironment(): CapsuleConfig {
        val env = System.getenv()

        return CapsuleConfig(
            input = InputConfig(
                outputDir = env["CAPSULE_INPUT_OUTPUT_DIR"] ?: "capsule",
                sliderScriptDir = env["CAPSULE_INPUT_SLIDER_SCRIPT_DIR"] ?: "capsule",
                deckSourceDir = env["CAPSULE_INPUT_DECK_SOURCE_DIR"] ?: "docs/asciidocRevealJs",
                chromiumExecutablePath = env["CAPSULE_INPUT_CHROMIUM_EXECUTABLE_PATH"] ?: ""
            ),
            tts = TtsConfig(
                engine = env["CAPSULE_TTS_ENGINE"] ?: "piper",
                voice = env["CAPSULE_TTS_VOICE"] ?: "fr_FR-siwis-medium",
                piperExecutablePath = env["CAPSULE_TTS_PIPER_EXECUTABLE_PATH"] ?: "piper",
                fallbackEnabled = env["CAPSULE_TTS_FALLBACK_ENABLED"]?.toBoolean() ?: true,
                espeakVoice = env["CAPSULE_TTS_ESPEAK_VOICE"] ?: "fr",
                espeakSpeed = env["CAPSULE_TTS_ESPEAK_SPEED"]?.toIntOrNull() ?: 150
            ),
            capture = CaptureConfig(
                viewportWidth = env["CAPSULE_CAPTURE_VIEWPORT_WIDTH"]?.toIntOrNull() ?: 1408,
                viewportHeight = env["CAPSULE_CAPTURE_VIEWPORT_HEIGHT"]?.toIntOrNull() ?: 792,
                playwrightTimeout = env["CAPSULE_CAPTURE_PLAYWRIGHT_TIMEOUT"]?.toDoubleOrNull() ?: 120_000.0,
                slideDurationSeconds = env["CAPSULE_CAPTURE_SLIDE_DURATION_SECONDS"]?.toDoubleOrNull() ?: 5.0,
                parallelCaptureEnabled = env["CAPSULE_CAPTURE_PARALLEL_CAPTURE_ENABLED"]?.toBoolean() ?: false,
                parallelCaptureThreads = env["CAPSULE_CAPTURE_PARALLEL_CAPTURE_THREADS"]?.toIntOrNull() ?: 4
            ),
            distrib = DistribConfig(
                ffmpegExecutablePath = env["CAPSULE_DISTRIB_FFMPEG_EXECUTABLE_PATH"] ?: "ffmpeg",
                outputWidth = env["CAPSULE_DISTRIB_OUTPUT_WIDTH"]?.toIntOrNull() ?: 1080,
                outputHeight = env["CAPSULE_DISTRIB_OUTPUT_HEIGHT"]?.toIntOrNull() ?: 1920
            ),
            manim = ManimConfig(
                executablePath = env["CAPSULE_MANIM_EXECUTABLE_PATH"] ?: "manim",
                quality = env["CAPSULE_MANIM_QUALITY"] ?: "l",
                scriptsDir = env["CAPSULE_MANIM_SCRIPTS_DIR"] ?: "src/manim",
                outputDir = env["CAPSULE_MANIM_OUTPUT_DIR"] ?: "build/capsule/manim",
                parallelRender = env["CAPSULE_MANIM_PARALLEL_RENDER"]?.toBoolean() ?: false,
                parallelRenderThreads = env["CAPSULE_MANIM_PARALLEL_RENDER_THREADS"]?.toIntOrNull() ?: 4
            )
        )
    }

    private fun buildConfigFromProperties(props: Map<String, String>): CapsuleConfig {
        return CapsuleConfig(
            input = InputConfig(
                outputDir = props["capsule.input.outputDir"] ?: "capsule",
                sliderScriptDir = props["capsule.input.sliderScriptDir"] ?: "capsule",
                deckSourceDir = props["capsule.input.deckSourceDir"] ?: "docs/asciidocRevealJs",
                chromiumExecutablePath = props["capsule.input.chromiumExecutablePath"] ?: ""
            ),
            tts = TtsConfig(
                engine = props["capsule.tts.engine"] ?: "piper",
                voice = props["capsule.tts.voice"] ?: "fr_FR-siwis-medium",
                piperExecutablePath = props["capsule.tts.piperExecutablePath"] ?: "piper",
                fallbackEnabled = props["capsule.tts.fallbackEnabled"]?.toBoolean() ?: true,
                espeakVoice = props["capsule.tts.espeakVoice"] ?: "fr",
                espeakSpeed = props["capsule.tts.espeakSpeed"]?.toIntOrNull() ?: 150
            ),
            capture = CaptureConfig(
                viewportWidth = props["capsule.capture.viewportWidth"]?.toIntOrNull() ?: 1408,
                viewportHeight = props["capsule.capture.viewportHeight"]?.toIntOrNull() ?: 792,
                playwrightTimeout = props["capsule.capture.playwrightTimeout"]?.toDoubleOrNull() ?: 120_000.0,
                slideDurationSeconds = props["capsule.capture.slideDurationSeconds"]?.toDoubleOrNull() ?: 5.0,
                parallelCaptureEnabled = props["capsule.capture.parallelCaptureEnabled"]?.toBoolean() ?: false,
                parallelCaptureThreads = props["capsule.capture.parallelCaptureThreads"]?.toIntOrNull() ?: 4
            ),
            distrib = DistribConfig(
                ffmpegExecutablePath = props["capsule.distrib.ffmpegExecutablePath"] ?: "ffmpeg",
                outputWidth = props["capsule.distrib.outputWidth"]?.toIntOrNull() ?: 1080,
                outputHeight = props["capsule.distrib.outputHeight"]?.toIntOrNull() ?: 1920
            ),
            manim = ManimConfig(
                executablePath = props["capsule.manim.executablePath"] ?: "manim",
                quality = props["capsule.manim.quality"] ?: "l",
                scriptsDir = props["capsule.manim.scriptsDir"] ?: "src/manim",
                outputDir = props["capsule.manim.outputDir"] ?: "build/capsule/manim",
                parallelRender = props["capsule.manim.parallelRender"]?.toBoolean() ?: false,
                parallelRenderThreads = props["capsule.manim.parallelRenderThreads"]?.toIntOrNull() ?: 4
            )
        )
    }

    // ─── Section merge methods ──────────────────────────────────

    // ─── Section merge methods ──────────────────────────────────
    //
    // Merge logic: CLI > YAML > Props > ENV
    // YAML always wins over props. Props always wins over ENV.
    // Empty-string fields use isNotBlank() as "explicitly set" heuristic.
    // Boolean/Int/Double fields: YAML/YAML-provided value always preferred over props.
    //

    private fun mergeInputConfig(env: InputConfig, props: InputConfig, yaml: InputConfig, cli: Map<String, Any?>): InputConfig {
        return InputConfig(
            outputDir = cli["input.outputDir"]?.toString() ?: yaml.outputDir.ifNotBlankOrElse(props.outputDir),
            sliderScriptDir = cli["input.sliderScriptDir"]?.toString() ?: yaml.sliderScriptDir.ifNotBlankOrElse(props.sliderScriptDir),
            deckSourceDir = cli["input.deckSourceDir"]?.toString() ?: yaml.deckSourceDir.ifNotBlankOrElse(props.deckSourceDir),
            chromiumExecutablePath = cli["input.chromiumExecutablePath"]?.toString()
                ?: yaml.chromiumExecutablePath.ifNotBlankOrElse(props.chromiumExecutablePath.ifNotBlankOrElse(env.chromiumExecutablePath))
        )
    }

    private fun mergeTtsConfig(env: TtsConfig, props: TtsConfig, yaml: TtsConfig, cli: Map<String, Any?>): TtsConfig {
        return TtsConfig(
            engine = cli["tts.engine"]?.toString() ?: yaml.engine.ifNotBlankOrElse(props.engine),
            voice = cli["tts.voice"]?.toString() ?: yaml.voice.ifNotBlankOrElse(props.voice),
            piperExecutablePath = cli["tts.piperExecutablePath"]?.toString() ?: yaml.piperExecutablePath.ifNotBlankOrElse(props.piperExecutablePath),
            fallbackEnabled = cli["tts.fallbackEnabled"]?.toString()?.toBoolean() ?: yaml.fallbackEnabled,
            espeakVoice = cli["tts.espeakVoice"]?.toString() ?: yaml.espeakVoice.ifNotBlankOrElse(props.espeakVoice),
            espeakSpeed = cli["tts.espeakSpeed"] as? Int ?: yaml.espeakSpeed
        )
    }

    private fun mergeCaptureConfig(env: CaptureConfig, props: CaptureConfig, yaml: CaptureConfig, cli: Map<String, Any?>): CaptureConfig {
        return CaptureConfig(
            viewportWidth = cli["capture.viewportWidth"] as? Int ?: yaml.viewportWidth,
            viewportHeight = cli["capture.viewportHeight"] as? Int ?: yaml.viewportHeight,
            playwrightTimeout = cli["capture.playwrightTimeout"] as? Double ?: yaml.playwrightTimeout,
            slideDurationSeconds = cli["capture.slideDurationSeconds"] as? Double ?: yaml.slideDurationSeconds,
            parallelCaptureEnabled = cli["capture.parallelCaptureEnabled"]?.toString()?.toBoolean() ?: yaml.parallelCaptureEnabled,
            parallelCaptureThreads = cli["capture.parallelCaptureThreads"] as? Int ?: yaml.parallelCaptureThreads
        )
    }

    private fun mergeDistribConfig(env: DistribConfig, props: DistribConfig, yaml: DistribConfig, cli: Map<String, Any?>): DistribConfig {
        return DistribConfig(
            ffmpegExecutablePath = cli["distrib.ffmpegExecutablePath"]?.toString() ?: yaml.ffmpegExecutablePath.ifNotBlankOrElse(props.ffmpegExecutablePath),
            outputWidth = cli["distrib.outputWidth"] as? Int ?: yaml.outputWidth,
            outputHeight = cli["distrib.outputHeight"] as? Int ?: yaml.outputHeight
        )
    }

    private fun mergeManimConfig(env: ManimConfig, props: ManimConfig, yaml: ManimConfig, cli: Map<String, Any?>): ManimConfig {
        return ManimConfig(
            executablePath = cli["manim.executablePath"]?.toString() ?: yaml.executablePath.ifNotBlankOrElse(props.executablePath),
            quality = cli["manim.quality"]?.toString() ?: yaml.quality.ifNotBlankOrElse(props.quality),
            scriptsDir = cli["manim.scriptsDir"]?.toString() ?: yaml.scriptsDir.ifNotBlankOrElse(props.scriptsDir),
            outputDir = cli["manim.outputDir"]?.toString() ?: yaml.outputDir.ifNotBlankOrElse(props.outputDir),
            parallelRender = cli["manim.parallelRender"]?.toString()?.toBoolean() ?: yaml.parallelRender,
            parallelRenderThreads = cli["manim.parallelRenderThreads"] as? Int ?: yaml.parallelRenderThreads
        )
    }

    /** Helper: if this string is not blank, return it; otherwise return [fallback]. */
    private fun String.ifNotBlankOrElse(fallback: String): String =
        if (this.isNotBlank()) this else fallback

    // ─── No-YAML merge methods (CLI > Props > ENV) ───────────────

    private fun mergeInputConfigNoYaml(env: InputConfig, props: InputConfig, cli: Map<String, Any?>): InputConfig {
        return InputConfig(
            outputDir = cli["input.outputDir"]?.toString() ?: props.outputDir.ifNotBlankOrElse(env.outputDir),
            sliderScriptDir = cli["input.sliderScriptDir"]?.toString() ?: props.sliderScriptDir.ifNotBlankOrElse(env.sliderScriptDir),
            deckSourceDir = cli["input.deckSourceDir"]?.toString() ?: props.deckSourceDir.ifNotBlankOrElse(env.deckSourceDir),
            chromiumExecutablePath = cli["input.chromiumExecutablePath"]?.toString()
                ?: props.chromiumExecutablePath.ifNotBlankOrElse(env.chromiumExecutablePath)
        )
    }

    private fun mergeTtsConfigNoYaml(env: TtsConfig, props: TtsConfig, cli: Map<String, Any?>): TtsConfig {
        return TtsConfig(
            engine = cli["tts.engine"]?.toString() ?: props.engine.ifNotBlankOrElse(env.engine),
            voice = cli["tts.voice"]?.toString() ?: props.voice.ifNotBlankOrElse(env.voice),
            piperExecutablePath = cli["tts.piperExecutablePath"]?.toString() ?: props.piperExecutablePath.ifNotBlankOrElse(env.piperExecutablePath),
            fallbackEnabled = cli["tts.fallbackEnabled"]?.toString()?.toBoolean() ?: props.fallbackEnabled,
            espeakVoice = cli["tts.espeakVoice"]?.toString() ?: props.espeakVoice.ifNotBlankOrElse(env.espeakVoice),
            espeakSpeed = cli["tts.espeakSpeed"] as? Int ?: props.espeakSpeed
        )
    }

    private fun mergeCaptureConfigNoYaml(env: CaptureConfig, props: CaptureConfig, cli: Map<String, Any?>): CaptureConfig {
        return CaptureConfig(
            viewportWidth = cli["capture.viewportWidth"] as? Int ?: props.viewportWidth,
            viewportHeight = cli["capture.viewportHeight"] as? Int ?: props.viewportHeight,
            playwrightTimeout = cli["capture.playwrightTimeout"] as? Double ?: props.playwrightTimeout,
            slideDurationSeconds = cli["capture.slideDurationSeconds"] as? Double ?: props.slideDurationSeconds,
            parallelCaptureEnabled = cli["capture.parallelCaptureEnabled"]?.toString()?.toBoolean() ?: props.parallelCaptureEnabled,
            parallelCaptureThreads = cli["capture.parallelCaptureThreads"] as? Int ?: props.parallelCaptureThreads
        )
    }

    private fun mergeDistribConfigNoYaml(env: DistribConfig, props: DistribConfig, cli: Map<String, Any?>): DistribConfig {
        return DistribConfig(
            ffmpegExecutablePath = cli["distrib.ffmpegExecutablePath"]?.toString() ?: props.ffmpegExecutablePath.ifNotBlankOrElse(env.ffmpegExecutablePath),
            outputWidth = cli["distrib.outputWidth"] as? Int ?: props.outputWidth,
            outputHeight = cli["distrib.outputHeight"] as? Int ?: props.outputHeight
        )
    }

    private fun mergeManimConfigNoYaml(env: ManimConfig, props: ManimConfig, cli: Map<String, Any?>): ManimConfig {
        return ManimConfig(
            executablePath = cli["manim.executablePath"]?.toString() ?: props.executablePath.ifNotBlankOrElse(env.executablePath),
            quality = cli["manim.quality"]?.toString() ?: props.quality.ifNotBlankOrElse(env.quality),
            scriptsDir = cli["manim.scriptsDir"]?.toString() ?: props.scriptsDir.ifNotBlankOrElse(env.scriptsDir),
            outputDir = cli["manim.outputDir"]?.toString() ?: props.outputDir.ifNotBlankOrElse(env.outputDir),
            parallelRender = cli["manim.parallelRender"]?.toString()?.toBoolean() ?: props.parallelRender,
            parallelRenderThreads = cli["manim.parallelRenderThreads"] as? Int ?: props.parallelRenderThreads
        )
    }
}