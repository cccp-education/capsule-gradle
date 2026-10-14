package capsule

import capsule.chapters.CardRenderer
import capsule.chapters.CardRendererImpl
import capsule.chapters.ChapterMarker
import capsule.chapters.ChapterMarkerImpl
import capsule.chapters.NoOpCardRenderer
import capsule.chapters.NoOpChapterMarker
import org.gradle.api.Project
import java.io.File

/**
 * Runtime factories for capsule (CAP-DECOMPOSE BS-1, S-195).
 *
 * Extracted verbatim from `CapsuleManager.companion` — a distinct responsibility
 * from task registration: select the concrete adapter for a capability
 * (Manim engine/mixer/replacer/parallel renderer, ffmpeg-backed services,
 * chapter/card writers) with safe degradation and `StrictModeGuard`.
 *
 * Naming mirrors the `resolve*` convention used across the capsule domains
 * (`resolveFormatConverter`, `resolveAudioPostProcessor`, …).
 */
object CapsuleFactory {

    /**
     * Resolves the appropriate ManimVideoMixer based on ffmpeg availability.
     * - If ffmpeg is not available, returns NoOpManimVideoMixer
     * - Otherwise, returns ManimVideoMixerImpl
     */
    @JvmStatic
    fun resolveManimVideoMixer(ffmpegPath: String = "ffmpeg", strict: Boolean = false): ManimVideoMixer {
        if (ffmpegPath == "noop") return NoOpManimVideoMixer()
        val mixer = ManimVideoMixerImpl(ffmpegPath)
        return if (mixer.isAvailable()) {
            mixer
        } else {
            StrictModeGuard.requireAvailable(strict, "ffmpeg (manim video mixer)", false, ffmpegPath)
            NoOpManimVideoMixer()
        }
    }

    /**
     * Resolves the appropriate ManimSlideReplacer.
     * Always returns ManimSlideReplacerImpl (always available, pure HTML manipulation).
     */
    @JvmStatic
    fun resolveManimSlideReplacer(): ManimSlideReplacer {
        return ManimSlideReplacerImpl()
    }

    /**
     * Resolves the appropriate ManimEngine based on configuration.
     * - If executablePath is "noop", returns NoOpManimEngine
     * - Otherwise, creates ManimEngineImpl(config) and falls back to NoOpManimEngine if unavailable
     */
    @JvmStatic
    fun resolveManimEngine(config: ManimConfig, strict: Boolean = false): ManimEngine {
        if (config.executablePath == "noop") {
            return NoOpManimEngine()
        }
        val engine = ManimEngineImpl(config)
        return if (engine.isAvailable()) {
            engine
        } else {
            StrictModeGuard.requireAvailable(strict, "manim", false, config.executablePath)
            NoOpManimEngine()
        }
    }

    /**
     * Resolves the appropriate ManimParallelRenderer based on parallelism.
     * - parallelism = 1: NoOpManimParallelRenderer (sequential fallback)
     * - parallelism > 1: ManimParallelRendererImpl with thread pool
     */
    @JvmStatic
    fun resolveManimParallelRenderer(parallelism: Int = 4): ManimParallelRenderer {
        return if (parallelism <= 1) {
            NoOpManimParallelRenderer()
        } else {
            ManimParallelRendererImpl(parallelism)
        }
    }

    /**
     * Resolves the appropriate SubtitleBurnInService based on ffmpeg availability.
     * - If ffmpegPath is "noop", returns NoOpSubtitleBurnInService
     * - Otherwise, returns SubtitleBurnInServiceImpl if ffmpeg is available
     */
    @JvmStatic
    fun resolveSubtitleBurnInService(ffmpegPath: String = "ffmpeg", style: SubtitleBurnInStyle = SubtitleBurnInStyle(), strict: Boolean = false): SubtitleBurnInService {
        if (ffmpegPath == "noop") return NoOpSubtitleBurnInService()
        val service = SubtitleBurnInServiceImpl(ffmpegPath, style)
        return if (service.isAvailable()) {
            service
        } else {
            StrictModeGuard.requireAvailable(strict, "ffmpeg (subtitle burn-in)", false, ffmpegPath)
            NoOpSubtitleBurnInService()
        }
    }

    /**
     * Resolves the appropriate [VideoFormatConverter] based on ffmpeg
     * availability (CAP-MP4 US-2). Pattern mirrors
     * [resolveSubtitleBurnInService]:
     * - If ffmpegPath is "noop", returns [NoOpVideoFormatConverter]
     * - Otherwise, returns [VideoFormatConverterImpl] if ffmpeg is available
     * - If unavailable and strict, [StrictModeGuard] throws
     * - If unavailable and non-strict, returns [NoOpVideoFormatConverter]
     *   (degraded mode — WebM kept, backward compat)
     */
    @JvmStatic
    fun resolveFormatConverter(ffmpegPath: String = "ffmpeg", strict: Boolean = false, bt709: Boolean = false): VideoFormatConverter {
        if (ffmpegPath == "noop") return NoOpVideoFormatConverter()
        val converter = VideoFormatConverterImpl(ffmpegPath, bt709)
        return if (converter.isAvailable()) {
            converter
        } else {
            StrictModeGuard.requireAvailable(strict, "ffmpeg (format conversion)", false, ffmpegPath)
            NoOpVideoFormatConverter()
        }
    }

    /**
     * Resolves the appropriate [capsule.audio.AudioPostProcessor]
     * based on ffmpeg availability (CAP-AUDIO US-2). 5ème factory
     * `resolve*`, pattern mirrors [resolveFormatConverter]:
     * - If ffmpegPath is "noop", returns [capsule.audio.NoOpAudioPostProcessor]
     * - Otherwise, returns [capsule.audio.AudioPostProcessorImpl] if ffmpeg is available
     * - If unavailable and strict, [StrictModeGuard] throws
     * - If unavailable and non-strict, returns [capsule.audio.NoOpAudioPostProcessor]
     *   (degraded mode — original video kept, backward compat)
     */
    @JvmStatic
    fun resolveAudioPostProcessor(ffmpegPath: String = "ffmpeg", strict: Boolean = false): capsule.audio.AudioPostProcessor {
        if (ffmpegPath == "noop") return capsule.audio.NoOpAudioPostProcessor()
        val processor = capsule.audio.AudioPostProcessorImpl(ffmpegPath)
        return if (processor.isAvailable()) {
            processor
        } else {
            StrictModeGuard.requireAvailable(strict, "ffmpeg (audio post)", false, ffmpegPath)
            capsule.audio.NoOpAudioPostProcessor()
        }
    }

    /**
     * Resolves the appropriate [capsule.podcast.PodcastConcatenator]
     * based on ffmpeg availability (CAP-PODCAST US-1). 8ème factory
     * `resolve*`, pattern mirrors [resolveAudioPostProcessor]:
     * - If ffmpegPath is "noop", returns [capsule.podcast.NoOpPodcastConcatenator]
     * - Otherwise, returns [capsule.podcast.PodcastConcatenatorImpl] if ffmpeg is available
     * - If unavailable and strict, [StrictModeGuard] throws
     * - If unavailable and non-strict, returns [capsule.podcast.NoOpPodcastConcatenator]
     *   (degraded mode — no podcast produced, backward compat)
     */
    @JvmStatic
    fun resolvePodcastConcatenator(ffmpegPath: String = "ffmpeg", strict: Boolean = false): capsule.podcast.PodcastConcatenator {
        if (ffmpegPath == "noop") return capsule.podcast.NoOpPodcastConcatenator()
        val concat = capsule.podcast.PodcastConcatenatorImpl(ffmpegPath)
        return if (concat.isAvailable()) {
            concat
        } else {
            StrictModeGuard.requireAvailable(strict, "ffmpeg (podcast concat)", false, ffmpegPath)
            capsule.podcast.NoOpPodcastConcatenator()
        }
    }

    /**
     * 9th factory — chapter marker metadata file writer.
     *
     * - noop → [NoOpChapterMarker] (writes empty JSON)
     * - otherwise → [ChapterMarkerImpl] (always available — disk write only, no ffmpeg)
     * - strict has no effect here (disk write always succeeds)
     */
    @JvmStatic
    fun resolveChapterMarker(ffmpegPath: String = "ffmpeg", strict: Boolean = false): ChapterMarker {
        if (ffmpegPath == "noop") return NoOpChapterMarker()
        return ChapterMarkerImpl(ffmpegPath)
    }

    /**
     * 10th factory — card renderer for intro/outro chapter cards.
     *
     * - noop → [NoOpCardRenderer] (writes no-op HTML placeholder)
     * - otherwise → [CardRendererImpl] (always available — disk write + HTML generation, no external deps)
     */
    @JvmStatic
    fun resolveCardRenderer(ffmpegPath: String = "ffmpeg", strict: Boolean = false): CardRenderer {
        if (ffmpegPath == "noop") return NoOpCardRenderer()
        return CardRendererImpl()
    }

    fun readScriptFiles(dir: File): List<File> {
        return dir.listFiles { f ->
            f.name.endsWith("-script.txt") &&
            !f.name.startsWith("example-") &&
            !f.name.contains("-context-")
        }
            ?.toList() ?: emptyList()
    }

    fun resolveScriptDir(project: Project, capsuleExt: CapsuleExtension): File {
        val configured = capsuleExt.sliderScriptDir.get()
        val candidate = project.layout.buildDirectory.dir(configured).get().asFile
        if (candidate.exists() && candidate.listFiles()
                ?.any { it.name.endsWith("-script.txt") } == true
        ) {
            return candidate
        }
        // CAP-CR3-4 — configurable fallback to slider's build output.
        val sliderBuildDir = capsuleExt.sliderBuildDir.orNull
        if (!sliderBuildDir.isNullOrBlank()) {
            val sliderOutput = project.file(sliderBuildDir)
                .resolve("slider").resolve("build").resolve("capsule")
            if (sliderOutput.exists()) return sliderOutput
        }
        return candidate
    }

    fun resolveDeckDir(project: Project, capsuleExt: CapsuleExtension): File {
        val configured = capsuleExt.deckSourceDir.get()
        val candidate = project.layout.buildDirectory.dir(configured).get().asFile
        if (candidate.exists()) return candidate
        // CAP-CR3-4 — configurable fallback to slider's build output.
        val sliderBuildDir = capsuleExt.sliderBuildDir.orNull
        if (!sliderBuildDir.isNullOrBlank()) {
            val sliderOutput = project.file(sliderBuildDir)
                .resolve("slider").resolve("build").resolve("docs").resolve("asciidocRevealJs")
            if (sliderOutput.exists()) return sliderOutput
        }
        return candidate
    }
}
