package capsule

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class CapsulePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        try {
            project.plugins.apply("education.cccp.slider")
        } catch (_: Exception) {
            project.logger.debug("slider-gradle not on classpath, skipping auto-apply")
        }

        val capsuleExt = project.extensions.create("capsule", CapsuleExtension::class.java)
        CapsuleManager(project).registerTasks()

        // After DSL is configured, merge 4 sources and push resolved values
        project.afterEvaluate {
            val configPath = capsuleExt.configPath.get()
            val configFile = File(project.projectDir, configPath)
            val yamlLoaded = configFile.exists() && configFile.length() > 0

            // Load YAML config (defaults if file doesn't exist)
            val yamlConfig = CapsuleConfigLoader.load(configFile)

            // Collect CLI -P params prefixed with "capsule."
            val cliParams = collectCliParams(project)

            // Merge: CLI > YAML > gradle.properties > ENV
            val mergedConfig = CapsuleConfigMerger.merge(project.projectDir, yamlConfig, cliParams, yamlLoaded)

            // Push resolved values into DSL Extension properties
            pushConfigIntoExtension(mergedConfig, capsuleExt)

            project.logger.lifecycle(
                "Capsule config resolved: engine={}, voice={}, vw={}x{}, parallel={}, manim=[path={}, quality={}, scriptsDir={}, outputDir={}]",
                mergedConfig.tts.engine, mergedConfig.tts.voice,
                mergedConfig.capture.viewportWidth, mergedConfig.capture.viewportHeight,
                mergedConfig.capture.parallelCaptureEnabled,
                mergedConfig.manim.executablePath, mergedConfig.manim.quality,
                mergedConfig.manim.scriptsDir, mergedConfig.manim.outputDir
            )
        }
    }

    /**
     * Collects CLI -P params that start with "capsule." and converts them
     * to the flat key format used by ConfigMerger (e.g. "capsule.tts.engine" → "tts.engine").
     */
    internal fun collectCliParams(project: Project): Map<String, Any?> {
        val cliParams = mutableMapOf<String, Any?>()
        val prefix = "capsule."
        project.properties.forEach { (key, value) ->
            if (key.startsWith(prefix)) {
                val flatKey = key.removePrefix(prefix)
                cliParams[flatKey] = value
            }
        }
        return cliParams
    }

    /**
     * Pushes the merged configuration values into the Gradle Extension properties.
     * Only pushes values that differ from the Extension's current convention defaults,
     * preserving any explicit DSL configuration from the build script.
     */
    internal fun pushConfigIntoExtension(config: CapsuleConfig, ext: CapsuleExtension) {
        // Input section
        ext.outputDir.set(config.input.outputDir)
        ext.sliderScriptDir.set(config.input.sliderScriptDir)
        ext.deckSourceDir.set(config.input.deckSourceDir)
        ext.chromiumExecutablePath.set(config.input.chromiumExecutablePath)

        // TTS section
        ext.ttsEngine.set(config.tts.engine)
        ext.ttsVoice.set(config.tts.voice)
        ext.piperExecutablePath.set(config.tts.piperExecutablePath)
        ext.ttsFallbackEnabled.set(config.tts.fallbackEnabled)
        ext.espeakVoice.set(config.tts.espeakVoice)
        ext.espeakSpeed.set(config.tts.espeakSpeed)

        // Capture section
        ext.viewportWidth.set(config.capture.viewportWidth)
        ext.viewportHeight.set(config.capture.viewportHeight)
        ext.playwrightTimeout.set(config.capture.playwrightTimeout)
        ext.slideDurationSeconds.set(config.capture.slideDurationSeconds)
        ext.parallelCaptureEnabled.set(config.capture.parallelCaptureEnabled)

        // Distrib section
        ext.ffmpegExecutablePath.set(config.distrib.ffmpegExecutablePath)
        ext.distribOutputWidth.set(config.distrib.outputWidth)
        ext.distribOutputHeight.set(config.distrib.outputHeight)

        // Manim section
        ext.manimExecutablePath.set(config.manim.executablePath)
        ext.manimQuality.set(config.manim.quality)
        ext.manimScriptsDir.set(config.manim.scriptsDir)
        ext.manimOutputDir.set(config.manim.outputDir)
        ext.manimParallelRender.set(config.manim.parallelRender)
    }
}
