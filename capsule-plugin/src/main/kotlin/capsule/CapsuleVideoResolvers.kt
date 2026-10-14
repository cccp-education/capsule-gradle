package capsule

import capsule.multilang.MultiLanguageResolver
import capsule.viral.ViralViewport
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Runtime resolvers for [CapsuleVideoTask] (CAP-DECOMPOSE BS-5, S-199).
 *
 * Extracted verbatim from the task — a distinct responsibility from the
 * orchestration sequence: select the concrete engine/service for a capability
 * (TTS engine, Manim engine/mixer/replacer/parallel renderer, subtitle burn-in
 * service), choose the capture viewport and the caption generator, and enumerate
 * the Manim output directories, with safe degradation and `StrictModeGuard`.
 *
 * Naming mirrors the `resolve*` convention used across the capsule domains and
 * [CapsuleFactory]. Stateless: the task keeps its cache/test seams and delegates
 * here, so this object performs no caching of its own.
 */
internal object CapsuleVideoResolvers {

    /**
     * Resolves the TTS engine from the extension (piper/espeak/fish/noop), with
     * `StrictModeGuard` and a safe NoOp fallback.
     */
    fun resolveTtsEngine(project: Project, ext: CapsuleExtension): TtsEngine {
        val langCode = ext.ttsLanguage.get()
        val resolvedLanguage = MultiLanguageResolver.resolve(langCode)?.language

        return when (ext.ttsEngine.get().lowercase()) {
            "piper" -> {
                val engine = PiperTtsEngine(
                    ext.piperExecutablePath.get(),
                    ext.ttsVoice.get(),
                    language = resolvedLanguage
                )
                if (engine.isAvailable()) {
                    engine
                } else {
                    StrictModeGuard.requireAvailable(
                        strict = ext.strictMode.get(),
                        engineName = "piper",
                        isAvailable = false,
                        path = ext.piperExecutablePath.get()
                    )
                    NoOpTtsEngine()
                }
            }
            "espeak" -> {
                val voice = ext.espeakVoice.get()
                val speed = ext.espeakSpeed.get()
                val engine = EspeakTtsEngine(voice = voice, speed = speed, language = resolvedLanguage)
                if (engine.isAvailable()) {
                    engine
                } else {
                    StrictModeGuard.requireAvailable(
                        strict = ext.strictMode.get(),
                        engineName = "espeak",
                        isAvailable = false,
                        path = "espeak"
                    )
                    NoOpTtsEngine()
                }
            }
            "fish", "fish-audio" -> {
                val keys = CapsuleConfigLoader.loadFishAudio(
                    project.file(ext.fishAudioConfigPath.get())
                )
                val params = FishAudioParams(
                    voice = ext.ttsFishVoice.get(),
                    model = ext.ttsFishModel.get(),
                    format = ext.ttsFishFormat.get(),
                    temperature = ext.ttsFishTemperature.get(),
                    topP = ext.ttsFishTopP.get(),
                    endpoint = ext.ttsFishEndpoint.get(),
                )
                val engine = FishAudioTtsEngine(
                    params, keys, ffmpegPath = ext.ffmpegExecutablePath.get(),
                )
                if (engine.isAvailable()) {
                    engine
                } else {
                    StrictModeGuard.requireAvailable(
                        strict = ext.strictMode.get(),
                        engineName = "fish",
                        isAvailable = false,
                        path = ext.fishAudioConfigPath.get()
                    )
                    NoOpTtsEngine()
                }
            }
            else -> NoOpTtsEngine()
        }
    }

    /** Resolves the Manim engine with a NoOp fallback (observable via [logger]). */
    fun resolveManimEngine(ext: CapsuleExtension, logger: Logger): ManimEngine {
        val config = ManimConfig(
            executablePath = ext.manimExecutablePath.get(),
            quality = ext.manimQuality.get(),
            scriptsDir = ext.manimScriptsDir.get(),
            outputDir = ext.manimOutputDir.get()
        )
        val engine = CapsuleFactory.resolveManimEngine(config, strict = ext.strictMode.get())
        if (engine.isAvailable()) {
            logger.lifecycle("Manim engine: {} (available)", engine.name())
        } else {
            logger.warn("Manim engine not available, using noop fallback")
        }
        return engine
    }

    /** Resolves the Manim video mixer with a NoOp fallback (observable via [logger]). */
    fun resolveManimVideoMixer(ext: CapsuleExtension, logger: Logger): ManimVideoMixer {
        val ffmpegPath = ext.ffmpegExecutablePath.get()
        val mixer = CapsuleFactory.resolveManimVideoMixer(ffmpegPath, strict = ext.strictMode.get())
        if (mixer.isAvailable()) {
            logger.lifecycle("Manim video mixer: {} (available)", mixer.name())
        } else {
            logger.warn("Manim video mixer not available, using noop fallback")
        }
        return mixer
    }

    /** Resolves the Manim slide replacer (always available, observable via [logger]). */
    fun resolveManimSlideReplacer(logger: Logger): ManimSlideReplacer {
        val replacer = CapsuleFactory.resolveManimSlideReplacer()
        logger.lifecycle("Manim slide replacer: {} (available)", replacer.name())
        return replacer
    }

    /**
     * Resolves the Manim parallel renderer: sequential (NoOp) at parallelism 1,
     * a thread-pool impl above (observable via [logger]).
     */
    fun resolveManimParallelRenderer(ext: CapsuleExtension, logger: Logger): ManimParallelRenderer {
        val parallelism = if (ext.manimParallelRender.get()) ext.manimParallelRenderThreads.get() else 1
        val renderer = CapsuleFactory.resolveManimParallelRenderer(parallelism)
        logger.lifecycle("Manim parallel renderer: {} (parallelism={})", renderer.name(), parallelism)
        return renderer
    }

    /**
     * Every directory that may hold a Manim render, deepest first.
     *
     * Manim nests its output under a quality folder whose name follows the
     * requested resolution (`792p30`, `1080p60`...), so the tree is walked
     * instead of assuming one.
     */
    fun manimVideoDirs(project: Project, ext: CapsuleExtension): List<File> {
        val roots = listOf(
            project.file(ext.manimOutputDir.get()),
            project.layout.buildDirectory.dir(ext.manimOutputDir.get()).get().asFile,
            project.file("build/manim"),
        ).filter { it.isDirectory }
        return roots.flatMap { root ->
            root.walkTopDown().maxDepth(4).filter { it.isDirectory }.toList()
        }.distinct()
    }

    /**
     * Resolves the capture viewport (CAP-VIRAL US-2). When the `viral` section
     * is enabled, the short-form 9:16 portrait viewport (1080×1920) is used;
     * otherwise the pedagogical landscape default is preserved (backward compat).
     */
    fun resolveViewport(ext: CapsuleExtension): Pair<Int, Int> =
        ViralViewport.resolve(
            viralEnabled = ext.viralEnabled.getOrElse(false),
            defaultWidth = ext.viewportWidth.get(),
            defaultHeight = ext.viewportHeight.get(),
        )

    /** Resolves the caption generator for a subtitle [format]. */
    fun resolveSubtitleGenerator(format: SubtitleFormat): SubtitleGenerator = when (format) {
        SubtitleFormat.VTT -> VttGenerator()
        SubtitleFormat.SRT -> SrtGenerator()
    }

    /**
     * Resolves the subtitle burn-in service from the extension style + ffmpeg
     * availability, with a NoOp fallback (observable via [logger]).
     */
    fun resolveSubtitleBurnInService(ext: CapsuleExtension, logger: Logger): SubtitleBurnInService {
        val ffmpegPath = ext.ffmpegExecutablePath.get()
        val style = SubtitleBurnInStyle(
            fontSize = ext.subtitleBurnInFontSize.get(),
            fontColor = ext.subtitleBurnInFontColor.get(),
            outlineColor = ext.subtitleBurnInOutlineColor.get(),
            position = ext.subtitleBurnInPosition.get()
        )
        val service = CapsuleFactory.resolveSubtitleBurnInService(ffmpegPath, style, strict = ext.strictMode.get())
        if (service.isAvailable()) {
            logger.lifecycle("Subtitle burn-in service: {} (available, style: fontSize={}, color={}, position={})", service.name(), style.fontSize, style.fontColor, style.position)
        } else {
            logger.warn("Subtitle burn-in service not available, using noop fallback")
        }
        return service
    }
}
