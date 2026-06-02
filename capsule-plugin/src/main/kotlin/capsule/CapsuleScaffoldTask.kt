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
    lateinit var capsuleExtension: CapsuleExtension

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

capture:
  # viewportWidth: 1408
  # viewportHeight: 792
  # playwrightTimeout: 120000.0
  # slideDurationSeconds: 5.0
  # parallelCaptureEnabled: false

distrib:
  # ffmpegExecutablePath: ffmpeg
  # outputWidth: 1080
  # outputHeight: 1920

manim:
  # executablePath: manim
  # quality: l
  # scriptsDir: src/manim
  # outputDir: build/capsule/manim
""".trimIndent()
    }
}