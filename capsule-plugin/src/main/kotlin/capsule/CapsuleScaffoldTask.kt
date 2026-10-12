package capsule

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Scaffolds a default capsule-context.yml configuration file.
 *
 * Creates the file only if it does not already exist, preserving any
 * existing customisation. The scaffold includes all configurable sections
 * with sensible defaults and inline comments.
 */
@DisableCachingByDefault(because = "Filesystem-bound: creates a scaffold YAML file")
open class CapsuleScaffoldTask : DefaultTask() {

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = _capsuleExtension ?: project.extensions.getByType(CapsuleExtension::class.java).also { _capsuleExtension = it }
        set(value) { _capsuleExtension = value }

    private var _capsuleExtension: CapsuleExtension? = null

    @TaskAction
    fun execute() {
        val configFile = File(project.projectDir, capsuleExtension.configPath.get())

        if (configFile.exists()) {
            logger.lifecycle("capsule-context.yml already exists at {} — skipping scaffold", configFile.absolutePath)
            return
        }

        configFile.parentFile?.mkdirs()

        configFile.writeText(DEFAULT_CAPSULE_CONTEXT_YML)

        logger.lifecycle("Scaffolded capsule-context.yml → {}", configFile.absolutePath)
    }

    companion object {
        internal val DEFAULT_CAPSULE_CONTEXT_YML = """
# capsule-context.yml — Capsule Gradle configuration
# Resolution order: ENV vars < gradle.properties < YAML file < CLI -P params
# See: https://github.com/cheroliv/capsule-gradle

input:
  # outputDir: capsule
  # sliderScriptDir: capsule
  # deckSourceDir: docs/asciidocRevealJs
  # chromiumExecutablePath: ""

tts:
  # engine: piper
  # voice: fr_FR-siwis-medium
  # piperExecutablePath: piper
  # fallbackEnabled: true
  # espeakVoice: fr
  # espeakSpeed: 150
  # language: fr
  # Fish Audio (engine: fish) — the API key pool lives in the git-ignored fish.yaml
  # fishVoice: <cloned voice reference_id>
  # fishModel: speech-1.6
  # fishFormat: wav
  # fishTemperature: 0.85
  # fishTopP: 0.88
  # fishEndpoint: https://api.fish.audio/v1/tts

capture:
  # viewportWidth: 1408
  # viewportHeight: 792
  # playwrightTimeout: 120000.0
  # slideDurationSeconds: 5.0
  # parallelCaptureEnabled: false
  # parallelCaptureThreads: 4
  # captureTimeoutMinutes: 5
  # subtitleEnabled: false
  # subtitleFormat: srt
  # subtitleBurnIn: false
  # subtitleBurnInFontSize: 24
  # subtitleBurnInFontColor: "&H00FFFFFF"
  # subtitleBurnInOutlineColor: "&H00000000"
  # subtitleBurnInPosition: bottom

distrib:
  # ffmpegExecutablePath: ffmpeg
  # outputWidth: 1080
  # outputHeight: 1920

manim:
  # executablePath: manim
  # quality: l
  # scriptsDir: src/manim
  # outputDir: build/capsule/manim
  # parallelRender: false
  # parallelRenderThreads: 4
""".trimIndent()
    }
}