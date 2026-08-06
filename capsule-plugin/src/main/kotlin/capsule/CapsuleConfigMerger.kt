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

        val yaml: CapsuleConfig? = if (yamlLoaded) yamlConfig else null

        return CapsuleConfig(
            input = mergeInputConfig(envConfig.input, propertiesConfig.input, yaml?.input, cliParams),
            tts = mergeTtsConfig(envConfig.tts, propertiesConfig.tts, yaml?.tts, cliParams),
            capture = mergeCaptureConfig(envConfig.capture, propertiesConfig.capture, yaml?.capture, cliParams),
            distrib = mergeDistribConfig(envConfig.distrib, propertiesConfig.distrib, yaml?.distrib, cliParams),
            manim = mergeManimConfig(envConfig.manim, propertiesConfig.manim, yaml?.manim, cliParams)
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
                espeakSpeed = env["CAPSULE_TTS_ESPEAK_SPEED"]?.toIntOrNull() ?: 150,
                language = env["CAPSULE_TTS_LANGUAGE"] ?: "fr"
            ),
            capture = CaptureConfig(
                viewportWidth = env["CAPSULE_CAPTURE_VIEWPORT_WIDTH"]?.toIntOrNull() ?: 1408,
                viewportHeight = env["CAPSULE_CAPTURE_VIEWPORT_HEIGHT"]?.toIntOrNull() ?: 792,
                playwrightTimeout = env["CAPSULE_CAPTURE_PLAYWRIGHT_TIMEOUT"]?.toDoubleOrNull() ?: 120_000.0,
                slideDurationSeconds = env["CAPSULE_CAPTURE_SLIDE_DURATION_SECONDS"]?.toDoubleOrNull() ?: 5.0,
                parallelCaptureEnabled = env["CAPSULE_CAPTURE_PARALLEL_CAPTURE_ENABLED"]?.toBoolean() ?: false,
                parallelCaptureThreads = env["CAPSULE_CAPTURE_PARALLEL_CAPTURE_THREADS"]?.toIntOrNull() ?: 4,
                captureTimeoutMinutes = env["CAPSULE_CAPTURE_TIMEOUT_MINUTES"]?.toIntOrNull() ?: 5,
                subtitleEnabled = env["CAPSULE_CAPTURE_SUBTITLE_ENABLED"]?.toBoolean() ?: false,
                subtitleFormat = env["CAPSULE_CAPTURE_SUBTITLE_FORMAT"] ?: "srt",
                subtitleBurnIn = env["CAPSULE_CAPTURE_SUBTITLE_BURN_IN"]?.toBoolean() ?: false,
                subtitleBurnInFontSize = env["CAPSULE_CAPTURE_SUBTITLE_BURN_IN_FONT_SIZE"]?.toIntOrNull() ?: 24,
                subtitleBurnInFontColor = env["CAPSULE_CAPTURE_SUBTITLE_BURN_IN_FONT_COLOR"] ?: "&H00FFFFFF",
                subtitleBurnInOutlineColor = env["CAPSULE_CAPTURE_SUBTITLE_BURN_IN_OUTLINE_COLOR"] ?: "&H00000000",
                subtitleBurnInPosition = env["CAPSULE_CAPTURE_SUBTITLE_BURN_IN_POSITION"] ?: "bottom"
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
                espeakSpeed = props["capsule.tts.espeakSpeed"]?.toIntOrNull() ?: 150,
                language = props["capsule.tts.language"] ?: "fr"
            ),
            capture = CaptureConfig(
                viewportWidth = props["capsule.capture.viewportWidth"]?.toIntOrNull() ?: 1408,
                viewportHeight = props["capsule.capture.viewportHeight"]?.toIntOrNull() ?: 792,
                playwrightTimeout = props["capsule.capture.playwrightTimeout"]?.toDoubleOrNull() ?: 120_000.0,
                slideDurationSeconds = props["capsule.capture.slideDurationSeconds"]?.toDoubleOrNull() ?: 5.0,
                parallelCaptureEnabled = props["capsule.capture.parallelCaptureEnabled"]?.toBoolean() ?: false,
                parallelCaptureThreads = props["capsule.capture.parallelCaptureThreads"]?.toIntOrNull() ?: 4,
                captureTimeoutMinutes = props["capsule.capture.captureTimeoutMinutes"]?.toIntOrNull() ?: 5,
                subtitleEnabled = props["capsule.capture.subtitleEnabled"]?.toBoolean() ?: false,
                subtitleFormat = props["capsule.capture.subtitleFormat"] ?: "srt",
                subtitleBurnIn = props["capsule.capture.subtitleBurnIn"]?.toBoolean() ?: false,
                subtitleBurnInFontSize = props["capsule.capture.subtitleBurnInFontSize"]?.toIntOrNull() ?: 24,
                subtitleBurnInFontColor = props["capsule.capture.subtitleBurnInFontColor"] ?: "&H00FFFFFF",
                subtitleBurnInOutlineColor = props["capsule.capture.subtitleBurnInOutlineColor"] ?: "&H00000000",
                subtitleBurnInPosition = props["capsule.capture.subtitleBurnInPosition"] ?: "bottom"
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
    //
    // Merge logic: CLI > YAML > Props > ENV
    // YAML always wins over props. Props always wins over ENV.
    // Empty-string fields use isNotBlank() as "explicitly set" heuristic.
    // Boolean/Int/Double fields: YAML/YAML-provided value always preferred over props.
    // When yaml == null (no YAML file found), props > ENV fallback applies.
    //

    private fun mergeInputConfig(env: InputConfig, props: InputConfig, yaml: InputConfig?, cli: Map<String, Any?>): InputConfig {
        return InputConfig(
            outputDir = mergeStr(cli, "input.outputDir", yaml?.outputDir, props.outputDir, env.outputDir),
            sliderScriptDir = mergeStr(cli, "input.sliderScriptDir", yaml?.sliderScriptDir, props.sliderScriptDir, env.sliderScriptDir),
            deckSourceDir = mergeStr(cli, "input.deckSourceDir", yaml?.deckSourceDir, props.deckSourceDir, env.deckSourceDir),
            chromiumExecutablePath = mergeStr(cli, "input.chromiumExecutablePath", yaml?.chromiumExecutablePath, props.chromiumExecutablePath, env.chromiumExecutablePath)
        )
    }

    private fun mergeTtsConfig(env: TtsConfig, props: TtsConfig, yaml: TtsConfig?, cli: Map<String, Any?>): TtsConfig {
        return TtsConfig(
            engine = mergeStr(cli, "tts.engine", yaml?.engine, props.engine, env.engine),
            voice = mergeStr(cli, "tts.voice", yaml?.voice, props.voice, env.voice),
            piperExecutablePath = mergeStr(cli, "tts.piperExecutablePath", yaml?.piperExecutablePath, props.piperExecutablePath, env.piperExecutablePath),
            fallbackEnabled = mergeBoolean(cli, "tts.fallbackEnabled", yaml?.fallbackEnabled, props.fallbackEnabled),
            espeakVoice = mergeStr(cli, "tts.espeakVoice", yaml?.espeakVoice, props.espeakVoice, env.espeakVoice),
            espeakSpeed = mergeInt(cli, "tts.espeakSpeed", yaml?.espeakSpeed, props.espeakSpeed),
            language = mergeStr(cli, "tts.language", yaml?.language, props.language, env.language)
        )
    }

    private fun mergeCaptureConfig(env: CaptureConfig, props: CaptureConfig, yaml: CaptureConfig?, cli: Map<String, Any?>): CaptureConfig {
        return CaptureConfig(
            viewportWidth = mergeInt(cli, "capture.viewportWidth", yaml?.viewportWidth, props.viewportWidth),
            viewportHeight = mergeInt(cli, "capture.viewportHeight", yaml?.viewportHeight, props.viewportHeight),
            playwrightTimeout = mergeDouble(cli, "capture.playwrightTimeout", yaml?.playwrightTimeout, props.playwrightTimeout),
            slideDurationSeconds = mergeDouble(cli, "capture.slideDurationSeconds", yaml?.slideDurationSeconds, props.slideDurationSeconds),
            parallelCaptureEnabled = mergeBoolean(cli, "capture.parallelCaptureEnabled", yaml?.parallelCaptureEnabled, props.parallelCaptureEnabled),
            parallelCaptureThreads = mergeInt(cli, "capture.parallelCaptureThreads", yaml?.parallelCaptureThreads, props.parallelCaptureThreads),
            captureTimeoutMinutes = mergeInt(cli, "capture.captureTimeoutMinutes", yaml?.captureTimeoutMinutes, props.captureTimeoutMinutes),
            subtitleEnabled = mergeBoolean(cli, "capture.subtitleEnabled", yaml?.subtitleEnabled, props.subtitleEnabled),
            subtitleFormat = mergeStr(cli, "capture.subtitleFormat", yaml?.subtitleFormat, props.subtitleFormat, env.subtitleFormat),
            subtitleBurnIn = mergeBoolean(cli, "capture.subtitleBurnIn", yaml?.subtitleBurnIn, props.subtitleBurnIn),
            subtitleBurnInFontSize = mergeInt(cli, "capture.subtitleBurnInFontSize", yaml?.subtitleBurnInFontSize, props.subtitleBurnInFontSize),
            subtitleBurnInFontColor = mergeStr(cli, "capture.subtitleBurnInFontColor", yaml?.subtitleBurnInFontColor, props.subtitleBurnInFontColor, env.subtitleBurnInFontColor),
            subtitleBurnInOutlineColor = mergeStr(cli, "capture.subtitleBurnInOutlineColor", yaml?.subtitleBurnInOutlineColor, props.subtitleBurnInOutlineColor, env.subtitleBurnInOutlineColor),
            subtitleBurnInPosition = mergeStr(cli, "capture.subtitleBurnInPosition", yaml?.subtitleBurnInPosition, props.subtitleBurnInPosition, env.subtitleBurnInPosition)
        )
    }

    private fun mergeDistribConfig(env: DistribConfig, props: DistribConfig, yaml: DistribConfig?, cli: Map<String, Any?>): DistribConfig {
        return DistribConfig(
            ffmpegExecutablePath = mergeStr(cli, "distrib.ffmpegExecutablePath", yaml?.ffmpegExecutablePath, props.ffmpegExecutablePath, env.ffmpegExecutablePath),
            outputWidth = mergeInt(cli, "distrib.outputWidth", yaml?.outputWidth, props.outputWidth),
            outputHeight = mergeInt(cli, "distrib.outputHeight", yaml?.outputHeight, props.outputHeight)
        )
    }

    private fun mergeManimConfig(env: ManimConfig, props: ManimConfig, yaml: ManimConfig?, cli: Map<String, Any?>): ManimConfig {
        return ManimConfig(
            executablePath = mergeStr(cli, "manim.executablePath", yaml?.executablePath, props.executablePath, env.executablePath),
            quality = mergeStr(cli, "manim.quality", yaml?.quality, props.quality, env.quality),
            scriptsDir = mergeStr(cli, "manim.scriptsDir", yaml?.scriptsDir, props.scriptsDir, env.scriptsDir),
            outputDir = mergeStr(cli, "manim.outputDir", yaml?.outputDir, props.outputDir, env.outputDir),
            parallelRender = mergeBoolean(cli, "manim.parallelRender", yaml?.parallelRender, props.parallelRender),
            parallelRenderThreads = mergeInt(cli, "manim.parallelRenderThreads", yaml?.parallelRenderThreads, props.parallelRenderThreads)
        )
    }

    // ─── Generic merge helpers (CLI > YAML > Props > ENV) ────────
    //
    // String fields: isNotBlank() is the "explicitly set" heuristic — an
    // explicit blank never overrides a non-blank lower-priority source.
    // Boolean/Int/Double fields: the YAML/props value is always preferred
    // over ENV (no "blank" concept for non-strings).

    private fun mergeStr(
        cli: Map<String, Any?>,
        key: String,
        yaml: String?,
        props: String,
        env: String
    ): String {
        val cliValue = cli[key]?.toString()
        if (!cliValue.isNullOrBlank()) return cliValue
        if (!yaml.isNullOrBlank()) return yaml
        if (props.isNotBlank()) return props
        return env
    }

    private fun mergeInt(
        cli: Map<String, Any?>,
        key: String,
        yaml: Int?,
        props: Int
    ): Int {
        cli.cliInt(key)?.let { return it }
        yaml?.let { return it }
        return props
    }

    private fun mergeDouble(
        cli: Map<String, Any?>,
        key: String,
        yaml: Double?,
        props: Double
    ): Double {
        cli.cliDouble(key)?.let { return it }
        yaml?.let { return it }
        return props
    }

    private fun mergeBoolean(
        cli: Map<String, Any?>,
        key: String,
        yaml: Boolean?,
        props: Boolean
    ): Boolean {
        cli.cliBoolean(key)?.let { return it }
        yaml?.let { return it }
        return props
    }

    /** Helper: if this string is not blank, return it; otherwise return [fallback]. */
    private fun String.ifNotBlankOrElse(fallback: String): String =
        if (this.isNotBlank()) this else fallback

    private fun Map<String, Any?>.cliInt(key: String): Int? =
        this[key]?.let { (it as? Int) ?: it.toString().toIntOrNull() }

    private fun Map<String, Any?>.cliDouble(key: String): Double? =
        this[key]?.let { (it as? Double) ?: it.toString().toDoubleOrNull() }

    private fun Map<String, Any?>.cliBoolean(key: String): Boolean? =
        this[key]?.let { (it as? Boolean) ?: it.toString().toBoolean() }
}