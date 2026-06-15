package capsule

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

@DisableCachingByDefault(because = "Filesystem-bound: reads script files and writes TTS placeholder audio")
open class CapsuleBuildTask : DefaultTask() {

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = _capsuleExtension ?: project.extensions.getByType(CapsuleExtension::class.java).also { _capsuleExtension = it }
        set(value) { _capsuleExtension = value }

    private var _capsuleExtension: CapsuleExtension? = null

    init {
        outputDir.convention(project.layout.buildDirectory.dir("capsule"))
    }

    @get:Internal
    internal var ttsEngine: TtsEngine? = null

    private fun resolveTtsEngine(): TtsEngine {
        if (ttsEngine != null) return ttsEngine!!

        val configuredEngine = capsuleExtension.ttsEngine.get()
        return when (configuredEngine.lowercase()) {
            "piper" -> {
                val piperPath = capsuleExtension.piperExecutablePath.get()
                val voice = capsuleExtension.ttsVoice.get()
                val engine = PiperTtsEngine(piperPath, voice)
                if (engine.isAvailable()) {
                    logger.lifecycle("TTS engine: piper → {}", piperPath)
                    engine
                } else if (capsuleExtension.ttsFallbackEnabled.get()) {
                    logger.warn("Piper not available at {}, falling back to noop placeholder", piperPath)
                    NoOpTtsEngine()
                } else {
                    throw TtsException("Piper not available at: $piperPath and fallback is disabled")
                }
            }
            "espeak" -> {
                val voice = capsuleExtension.espeakVoice.get()
                val speed = capsuleExtension.espeakSpeed.get()
                val engine = EspeakTtsEngine(voice = voice, speed = speed)
                if (engine.isAvailable()) {
                    logger.lifecycle("TTS engine: espeak (voice={}, speed={})", voice, speed)
                    engine
                } else {
                    logger.warn("espeak not available, falling back to noop placeholder")
                    NoOpTtsEngine()
                }
            }
            "noop" -> {
                logger.lifecycle("TTS engine: noop (placeholder)")
                NoOpTtsEngine()
            }
            else -> {
                logger.warn("Unknown TTS engine '{}', using noop placeholder", configuredEngine)
                NoOpTtsEngine()
            }
        }
    }

    @TaskAction
    fun execute() {
        val scriptDir = CapsuleManager.resolveScriptDir(project, capsuleExtension)
        val scripts = CapsuleManager.readScriptFiles(scriptDir)

        if (scripts.isEmpty()) {
            logger.warn("No capsule scripts to process. Skipping TTS generation.")
            return
        }

        val outDir = project.layout.buildDirectory.dir(
            capsuleExtension.outputDir.get()
        ).get().asFile
        outDir.mkdirs()

        val engine = resolveTtsEngine()
        logger.lifecycle("TTS engine: {}", engine.name())

        val cores = Runtime.getRuntime().availableProcessors()
        val executor = Executors.newFixedThreadPool(cores)
        val synthesized = AtomicInteger(0)
        val failed = AtomicInteger(0)
        val futures = mutableListOf<Future<*>>()

        for (script in scripts) {
            val parsed = CapsuleManager.parseScript(script)
            val deckOutputDir = outDir.resolve(parsed.deckName)
            deckOutputDir.mkdirs()

            for (seg in parsed.slides) {
                val idx = String.format("%02d", seg.index)
                val ttsFile = deckOutputDir.resolve("slide-$idx.mp3")

                futures.add(executor.submit {
                    try {
                        engine.synthesize(seg.speakerNote, ttsFile)
                        synthesized.incrementAndGet()
                        logger.lifecycle("  TTS → {} ({} chars)", ttsFile.name, seg.speakerNote.length)
                    } catch (e: TtsException) {
                        failed.incrementAndGet()
                        logger.error("  TTS FAILED slide {}: {}", seg.index, e.message)
                    }
                })
            }
        }

        futures.forEach { it.get() }
        executor.shutdown()

        if (failed.get() > 0 && !capsuleExtension.ttsFallbackEnabled.get()) {
            throw TtsException("${failed.get()} TTS synthesis failures (fallback disabled)")
        }

        logger.lifecycle(
            "TTS generation: {} synthesized, {} failed, {} engine, {} cores",
            synthesized.get(), failed.get(), engine.name(), cores
        )
    }
}
