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
                "Capsule config resolved: engine={}, voice={}, language={}, vw={}x{}, parallel={}, subtitle={}/{}/burnIn={} manim=[path={}, quality={}, scriptsDir={}, outputDir={}]",
                mergedConfig.tts.engine, mergedConfig.tts.voice, mergedConfig.tts.language,
                mergedConfig.capture.viewportWidth, mergedConfig.capture.viewportHeight,
                mergedConfig.capture.parallelCaptureEnabled,
                mergedConfig.capture.subtitleEnabled, mergedConfig.capture.subtitleFormat, mergedConfig.capture.subtitleBurnIn,
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
     * Only sets properties that still hold their original convention value,
     * preserving explicit DSL configuration: DSL > CLI > YAML > props > ENV.
     */
    internal fun pushConfigIntoExtension(config: CapsuleConfig, ext: CapsuleExtension) {
        val conventions = ext.conventions

        // Input section — only fill if still at convention (DSL didn't override)
        if (ext.outputDir.get() == conventions.outputDir) ext.outputDir.set(config.input.outputDir)
        if (ext.sliderScriptDir.get() == conventions.sliderScriptDir) ext.sliderScriptDir.set(config.input.sliderScriptDir)
        if (ext.deckSourceDir.get() == conventions.deckSourceDir) ext.deckSourceDir.set(config.input.deckSourceDir)
        if (ext.chromiumExecutablePath.get() == conventions.chromiumExecutablePath) ext.chromiumExecutablePath.set(config.input.chromiumExecutablePath)

        // TTS section
        if (ext.ttsEngine.get() == conventions.ttsEngine) ext.ttsEngine.set(config.tts.engine)
        if (ext.ttsVoice.get() == conventions.ttsVoice) ext.ttsVoice.set(config.tts.voice)
        if (ext.piperExecutablePath.get() == conventions.piperExecutablePath) ext.piperExecutablePath.set(config.tts.piperExecutablePath)
        if (ext.ttsFallbackEnabled.get() == conventions.ttsFallbackEnabled) ext.ttsFallbackEnabled.set(config.tts.fallbackEnabled)
        if (ext.espeakVoice.get() == conventions.espeakVoice) ext.espeakVoice.set(config.tts.espeakVoice)
        if (ext.espeakSpeed.get() == conventions.espeakSpeed) ext.espeakSpeed.set(config.tts.espeakSpeed)
        if (ext.ttsLanguage.get() == conventions.ttsLanguage) ext.ttsLanguage.set(config.tts.language)

        // Capture section
        if (ext.viewportWidth.get() == conventions.viewportWidth) ext.viewportWidth.set(config.capture.viewportWidth)
        if (ext.viewportHeight.get() == conventions.viewportHeight) ext.viewportHeight.set(config.capture.viewportHeight)
        if (ext.playwrightTimeout.get() == conventions.playwrightTimeout) ext.playwrightTimeout.set(config.capture.playwrightTimeout)
        if (ext.slideDurationSeconds.get() == conventions.slideDurationSeconds) ext.slideDurationSeconds.set(config.capture.slideDurationSeconds)
        if (ext.parallelCaptureEnabled.get() == conventions.parallelCaptureEnabled) ext.parallelCaptureEnabled.set(config.capture.parallelCaptureEnabled)
        if (ext.parallelCaptureThreads.get() == conventions.parallelCaptureThreads) ext.parallelCaptureThreads.set(config.capture.parallelCaptureThreads)
        if (ext.subtitleEnabled.get() == conventions.subtitleEnabled) ext.subtitleEnabled.set(config.capture.subtitleEnabled)
        if (ext.subtitleFormat.get() == conventions.subtitleFormat) ext.subtitleFormat.set(config.capture.subtitleFormat)
        if (ext.subtitleBurnIn.get() == conventions.subtitleBurnIn) ext.subtitleBurnIn.set(config.capture.subtitleBurnIn)
        if (ext.subtitleBurnInFontSize.get() == conventions.subtitleBurnInFontSize) ext.subtitleBurnInFontSize.set(config.capture.subtitleBurnInFontSize)
        if (ext.subtitleBurnInFontColor.get() == conventions.subtitleBurnInFontColor) ext.subtitleBurnInFontColor.set(config.capture.subtitleBurnInFontColor)
        if (ext.subtitleBurnInOutlineColor.get() == conventions.subtitleBurnInOutlineColor) ext.subtitleBurnInOutlineColor.set(config.capture.subtitleBurnInOutlineColor)
        if (ext.subtitleBurnInPosition.get() == conventions.subtitleBurnInPosition) ext.subtitleBurnInPosition.set(config.capture.subtitleBurnInPosition)

        // Distrib section
        if (ext.ffmpegExecutablePath.get() == conventions.ffmpegExecutablePath) ext.ffmpegExecutablePath.set(config.distrib.ffmpegExecutablePath)
        if (ext.distribOutputWidth.get() == conventions.distribOutputWidth) ext.distribOutputWidth.set(config.distrib.outputWidth)
        if (ext.distribOutputHeight.get() == conventions.distribOutputHeight) ext.distribOutputHeight.set(config.distrib.outputHeight)

        // Manim section
        if (ext.manimExecutablePath.get() == conventions.manimExecutablePath) ext.manimExecutablePath.set(config.manim.executablePath)
        if (ext.manimQuality.get() == conventions.manimQuality) ext.manimQuality.set(config.manim.quality)
        if (ext.manimScriptsDir.get() == conventions.manimScriptsDir) ext.manimScriptsDir.set(config.manim.scriptsDir)
        if (ext.manimOutputDir.get() == conventions.manimOutputDir) ext.manimOutputDir.set(config.manim.outputDir)
        if (ext.manimParallelRender.get() == conventions.manimParallelRender) ext.manimParallelRender.set(config.manim.parallelRender)
        if (ext.manimParallelRenderThreads.get() == conventions.manimParallelRenderThreads) ext.manimParallelRenderThreads.set(config.manim.parallelRenderThreads)
    }
}
